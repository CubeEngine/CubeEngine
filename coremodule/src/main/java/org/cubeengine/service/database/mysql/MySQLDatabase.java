/**
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
/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 * <p/>
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.service.database.mysql;

import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.logscribe.LogFactory;
import de.cubeisland.engine.logscribe.LogLevel;
import de.cubeisland.engine.logscribe.LogTarget;
import de.cubeisland.engine.logscribe.filter.PrefixFilter;
import de.cubeisland.engine.logscribe.target.file.AsyncFileTarget;
import de.cubeisland.engine.modularity.core.Modularity;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.ModuleHandler;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.asm.marker.ServiceImpl;
import org.cubeengine.service.database.ModuleTables;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.service.logging.LoggingUtil;
import org.cubeengine.module.core.CoreModule;
import org.cubeengine.module.core.util.Version;
import org.cubeengine.service.database.AbstractDatabase;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.database.DatabaseConfiguration;
import org.cubeengine.service.database.Table;
import org.cubeengine.service.database.TableCreator;
import org.cubeengine.service.database.TableUpdateCreator;
import de.cubeisland.engine.reflect.Reflector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.MappedTable;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;

@ServiceImpl(Database.class)
@de.cubeisland.engine.modularity.asm.marker.Version(1)
public class MySQLDatabase extends AbstractDatabase implements Database, ModuleHandler
{
    private final MySQLDatabaseConfiguration config;
    private final HikariDataSource dataSource;

    private final Settings settings;
    private final MappedSchema mappedSchema;
    private Log logger;
    private final JooqLogger jooqLogger = new JooqLogger(this);

    @Inject
    public MySQLDatabase(Reflector reflector, File pluginFolder, Log logger, FileManager fm, LogFactory logFactory, Modularity modularity)
    {
        // Disable HikariPool Debug ConsoleSpam
        ((Logger)LogManager.getLogger(HikariPool.class)).setLevel(Level.INFO);
        ((Logger)LogManager.getLogger("com.zaxxer.hikari.pool.PoolBase")).setLevel(Level.INFO); // really? now pkg-private
        ((Logger)LogManager.getLogger(HikariConfig.class)).setLevel(Level.INFO);

        // Setting up Logger...
        this.logger = logger;
        AsyncFileTarget target = new AsyncFileTarget(LoggingUtil.getLogFile(fm, "Database"),
                                                     LoggingUtil.getFileFormat(true, false),
                                                     true, LoggingUtil.getCycler(), threadFactory);
        target.setLevel(LogLevel.DEBUG);
        logger.addTarget(target);

        LogTarget parentTarget = logger.addDelegate(logFactory.getLog(CoreModule.class));
        parentTarget.appendFilter(new PrefixFilter("[DB] "));
        parentTarget.setLevel(LogLevel.INFO);

        // Now go connect to the database:
        this.logger.info("Connecting to the database...");
        this.config = reflector.load(MySQLDatabaseConfiguration.class, new File(pluginFolder, "database.yml"));

        HikariConfig dsConf = new HikariDataSource();
        dsConf.setPoolName("CubeEngine");
        dsConf.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");

        dsConf.addDataSourceProperty("user", config.user);
        dsConf.addDataSourceProperty("password", config.password);
        dsConf.addDataSourceProperty("url", "jdbc:mysql://" + config.host + ":" + config.port + "/" + config.database);
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
            throw new IllegalStateException("Could not establish connection with the database!", e);
        }

        this.mappedSchema = new MappedSchema().withInput(config.database);
        this.settings = new Settings();
        this.settings.withRenderMapping(new RenderMapping().withSchemata(this.mappedSchema));
        this.settings.setExecuteLogging(false);

        this.logger.info("connected!");

        modularity.registerHandler(this);
    }

    private boolean updateTableStructure(TableUpdateCreator updater)
    {
        final String query = "SELECT table_name, table_comment FROM INFORMATION_SCHEMA.TABLES "
            + "WHERE table_schema = ? AND table_name = ?";
        try
        {
            ResultSet resultSet = query(query, this.config.database, updater.getName()).get();
            if (resultSet.next())
            {
                Version dbVersion = Version.fromString(resultSet.getString("table_comment"));
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
                    logger.info("{} got updated to {}", updater.getName(), version.toString());
                    execute("ALTER TABLE " + updater.getName() + " COMMENT = ?", version.toString());
                }
                return true;
            }
        }
        catch (InterruptedException | ExecutionException | SQLException e)
        {
            logger.warn(e, "Could not execute structure update for the table {}", updater.getName());
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
            Connection connection = this.getConnection();
            table.createTable(connection);
            connection.close();
        }
        catch (SQLException ex)
        {
            throw new IllegalStateException("Cannot create table " + table.getName(), ex);
        }
    }

    @Override
    public void registerTable(Class<? extends Table<?>> clazz)
    {
        try
        {
            Constructor<? extends Table<?>> constructor = clazz.getDeclaredConstructor(String.class, Database.class);
            Table<?> table = constructor.newInstance(this.config.tablePrefix, this);
            this.registerTable(table);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Unable to instantiate Table! " + clazz.getName(), e);
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
        return DSL.using(new DefaultConfiguration().set(SQLDialect.MYSQL).set(new DataSourceConnectionProvider(
            this.dataSource)).set(jooqLogger).set(settings));
    }

    @Override
    @Disable
    public void shutdown()
    {
        super.shutdown();
        this.dataSource.shutdown();
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

    @Override
    public void onEnable(Module module)
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

    @Override
    public void onDisable(Module module)
    {
        // nothing here
    }
}
