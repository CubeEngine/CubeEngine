/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.sql.database.mysql;

import static org.cubeengine.module.sql.database.TableVersion.TABLE_VERSION;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Injector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import org.cubeengine.libcube.service.ModuleInjector;
import org.cubeengine.logscribe.Log;
import org.cubeengine.logscribe.LogFactory;
import org.cubeengine.logscribe.LogLevel;
import org.cubeengine.logscribe.LogTarget;
import org.cubeengine.logscribe.filter.PrefixFilter;
import org.cubeengine.logscribe.target.file.AsyncFileTarget;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.module.sql.database.AbstractDatabase;
import org.cubeengine.module.sql.database.Database;
import org.cubeengine.module.sql.database.DatabaseConfiguration;
import org.cubeengine.module.sql.database.ModuleTables;
import org.cubeengine.module.sql.database.Table;
import org.cubeengine.module.sql.database.TableCreator;
import org.cubeengine.module.sql.database.TableUpdateCreator;
import org.cubeengine.module.sql.database.TableVersion;
import org.cubeengine.reflect.Reflector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.cubeengine.libcube.util.Version;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.logging.LoggingUtil;
import org.jooq.*;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.MappedTable;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.*;

@Singleton
public class MySQLDatabase extends AbstractDatabase implements Database, ModuleInjector<ModuleTables>
{
    private final MySQLDatabaseConfiguration config;
    private final HikariDataSource dataSource;

    private final Settings settings;
    private final MappedSchema mappedSchema;
    private Log logger;
    private final JooqLogger jooqLogger = new JooqLogger(this);

    @Inject
    public MySQLDatabase(Reflector reflector, ModuleManager mm, FileManager fm, LogFactory logFactory)
    {
        File pluginFolder = mm.getBasePath();

        // Disable HikariPool Debug ConsoleSpam
        ((Logger)LogManager.getLogger(HikariPool.class)).setLevel(Level.INFO);
        ((Logger)LogManager.getLogger("com.zaxxer.hikari.pool.PoolBase")).setLevel(Level.INFO); // really? now pkg-private
        ((Logger)LogManager.getLogger(HikariConfig.class)).setLevel(Level.INFO);

        // Setting up Logger...
        this.logger = mm.getLoggerFor(Database.class);
        AsyncFileTarget target =
                new AsyncFileTarget.Builder(LoggingUtil.getLogFile(fm, "Database").toPath(),
                        LoggingUtil.getFileFormat(true, false)
                ).setAppend(true).setCycler(LoggingUtil.getCycler()).setThreadFactory(threadFactory).build();

        target.setLevel(LogLevel.DEBUG);
        logger.addTarget(target);

        LogTarget parentTarget = logger.addDelegate(logFactory.getLog(LogFactory.class));
        parentTarget.appendFilter(new PrefixFilter("[DB] "));
        parentTarget.setLevel(LogLevel.INFO);

        // Now go connect to the database:
        this.logger.info("Connecting to the database...");
        this.config = reflector.load(MySQLDatabaseConfiguration.class, new File(pluginFolder, "database.yml"));

        HikariConfig dsConf = new HikariDataSource();
        dsConf.setPoolName("CubeEngine");
        //dsConf.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        dsConf.setJdbcUrl("jdbc:mysql://" + config.host + ":" + config.port + "/" + config.database);

        dsConf.addDataSourceProperty("user", config.user);
        dsConf.addDataSourceProperty("password", config.password);
        dsConf.addDataSourceProperty("databaseName", config.database);
        dsConf.addDataSourceProperty("cachePrepStmts", "true");
        dsConf.addDataSourceProperty("prepStmtCacheSize", "250");
        dsConf.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dsConf.addDataSourceProperty("useServerPrepStmts", "true");
        dsConf.addDataSourceProperty("useUnicode", "yes");
        dsConf.addDataSourceProperty("characterEncoding", "UTF-8");
        dsConf.addDataSourceProperty("connectionCollation", "utf8_general_ci");
        dsConf.setMinimumIdle(5);
        dsConf.setMaximumPoolSize(20);
        dsConf.setThreadFactory(threadFactory);
        dsConf.setConnectionTimeout(10000); // 10s
        dsConf.setInitializationFailFast(false);
        dataSource = new HikariDataSource(dsConf);
        try (Connection connection = dataSource.getConnection())
        {
            try (PreparedStatement s = connection.prepareStatement("SHOW variables WHERE Variable_name='wait_timeout'"))
            {
                ResultSet result = s.executeQuery();
                if (result.next())
                {
                    final int TIMEOUT_DELTA = 60;
                    int second = result.getInt("Value") - TIMEOUT_DELTA;
                    if (second <= 0)
                    {
                        second += TIMEOUT_DELTA;
                    }
                    dataSource.setIdleTimeout(second);
                    dataSource.setMaxLifetime(second);
                }
            }
        }
        catch (SQLException e)
        {
            logger.error("Could not establish connection with the database!");
            throw new IllegalStateException("Could not establish connection with the database!", e);
        }

        this.mappedSchema = new MappedSchema().withInput(config.database);
        this.settings = new Settings();
        this.settings.withRenderMapping(new RenderMapping().withSchemata(this.mappedSchema));
        this.settings.setExecuteLogging(false);

        this.logger.info("connected!");

        this.registerTable(new TableVersion());
    }

    private boolean updateTableStructure(TableUpdateCreator updater)
    {
        Record1<String> result = getDSL().select(TABLE_VERSION.VERSION).from(TABLE_VERSION).where(TABLE_VERSION.NAME.eq(updater.getName())).fetchOne();
        if (result != null)
        {
            try
            {
                Version dbVersion = Version.fromString(result.value1());
                Version version = updater.getTableVersion();
                if (dbVersion.isNewerThan(version))
                {
                    logger.info("table-version is newer than expected! {}: {} expected version: {}", updater.getName(),
                            dbVersion.toString(), version.toString());
                }
                else if (dbVersion.isOlderThan(updater.getTableVersion()))
                {
                    logger.info("table-version is too old! Updating {} from {} to {}", updater.getName(),
                            dbVersion.toString(), version.toString());
                    try (Connection connection = this.getConnection())
                    {
                        updater.update(connection, dbVersion);
                    }
                    getDSL().mergeInto(TABLE_VERSION).values(updater.getName(), version.toString());
                    logger.info("{} got updated to {}", updater.getName(), version.toString());
                }
                return true;
            }
            catch (SQLException e)
            {
                logger.warn(e, "Could not execute structure update for the table {}", updater.getName());
            }
        }
        return false;
    }

    /**
     * Creates or updates the table for given entity
     *
     * @param table
     */
    @Override
    public void registerTable(TableCreator<?> table)
    {
        initializeTable(table);
        final String name = table.getName();
        registerTableMapping(name);
        logger.debug("Database-Table {0} registered!", name);
    }

    private void registerTableMapping(String name)
    {
        for (final MappedTable mappedTable : this.mappedSchema.getTables())
        {
            if (name.equals(mappedTable.getInput()))
            {
                return;
            }
        }
        this.mappedSchema.withTables(new MappedTable().withInput(name).withOutput(getTablePrefix() + name));
    }

    protected void initializeTable(TableCreator<?> table)
    {
        if (table instanceof TableUpdateCreator && this.updateTableStructure((TableUpdateCreator)table))
        {
            return;
        }
        try
        {
            table.createTable(this);
        }
        catch (SQLException ex)
        {
            throw new IllegalStateException("Cannot create table " + table.getName(), ex);
        }
    }

    @Override
    public void registerTable(Class<? extends org.cubeengine.module.sql.database.Table<?>> clazz)
    {
        try
        {
            this.registerTable(clazz.newInstance());
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Unable to instantiate Table! " + clazz.getName(), e);
        }
    }

    @Override
    public void inject(Injector moduleInjector, Object instance, ModuleTables annotation) {
        for (Class<? extends Table<?>> table : annotation.value()) {
            this.registerTable(table);
        }
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return this.dataSource.getConnection();
    }

    @Override
    public DSLContext getDSL()
    {
        Configuration conf = new DefaultConfiguration()
                .set(SQLDialect.MYSQL)
                .set(new DataSourceConnectionProvider(this.dataSource))
                .set(new DefaultExecuteListenerProvider(jooqLogger))
                .set(settings)
                .set(new DefaultVisitListenerProvider(new TablePrefixer(getTablePrefix())));
        return DSL.using(conf);
    }

    @Override
    // TODO call on close
    public void shutdown()
    {
        super.shutdown();
        this.dataSource.close();
    }

    @Override
    public String getName()
    {
        return "MySQL";
    }

    @Override
    public DatabaseConfiguration getDatabaseConfig()
    {
        return this.config;
    }

    @Override
    public String getTablePrefix()
    {
        return this.config.tablePrefix;
    }

    @Override
    public Log getLog()
    {
        return logger;
    }

    // TODO register tables
    public void onEnable(Object module)
    {
        ModuleTables annotation = module.getClass().getAnnotation(ModuleTables.class);
        if (annotation != null)
        {
            for (Class<? extends Table<?>> tableClass : annotation.value())
            {
                registerTable(tableClass);
            }
        }
    }
}
