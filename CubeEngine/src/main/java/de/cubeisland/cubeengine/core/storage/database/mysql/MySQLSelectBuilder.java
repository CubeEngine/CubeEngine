package de.cubeisland.cubeengine.core.storage.database.mysql;

import de.cubeisland.cubeengine.core.storage.database.querybuilder.SelectBuilder;
import org.apache.commons.lang.Validate;

/**
 * MYSQLQueryBuilder for selecting from tables.
 */
public class MySQLSelectBuilder extends MySQLConditionalBuilder<SelectBuilder> implements SelectBuilder
{
    protected MySQLSelectBuilder(MySQLQueryBuilder parent)
    {
        super(parent);
    }

    @Override
    public MySQLSelectBuilder select()
    {
        if (this.query != null)
        {
            throw new IllegalStateException("Cannot create a nested SELECT query!");
        }
        this.query = new StringBuilder("SELECT ");
        return this;
    }

    @Override
    public MySQLSelectBuilder cols(String... cols)
    {
        if (cols.length > 0)
        {
            this.query.append(this.database.prepareFieldName(cols[0]));
            for (int i = 1; i < cols.length; ++i)
            {
                this.query.append(',').append(this.database.prepareFieldName(cols[i]));
            }
        }
        return this;
    }

    @Override
    public MySQLSelectBuilder from(String... tables)
    {
        Validate.notEmpty(tables, "No tables specified!");
        this.query.append(" \nFROM ").append(this.database.prepareTableName(tables[0]));
        for (int i = 1; i < tables.length; ++i)
        {
            this.query.append(',').append(this.database.prepareTableName(tables[i]));
        }
        return this;
    }

    @Override
    public MySQLSelectBuilder distinct()
    {
        this.query.append(" DISTINCT");
        return this;
    }

    @Override
    public MySQLSelectBuilder union(boolean all)
    {
        this.query.append(" UNION ");
        return this;
    }

    @Override
    public SelectBuilder into(String table)
    {
        this.query.append(" INTO ").append(this.database.prepareTableName(table));
        return this;
    }

    @Override
    public SelectBuilder in(String database)
    {
        this.query.append(" IN ").append(this.database.prepareFieldName(database));
        return this;
    }

    @Override
    public SelectBuilder leftJoinOnEqual(String table, String key, String otherTable, String otherKey)
    {
        this.query.append(" \nLEFT JOIN ").append(this.database.prepareTableName(table));
        this.onEqual(table, key, otherTable, otherKey);
        return this;
    }

    private void onEqual(String table, String key, String otherTable, String otherKey)
    {
        this.query.append(" ON ").append(this.database.prepareFieldName(table + "." + key))
                .append(" = ").append(this.database.prepareFieldName(otherTable + "." + otherKey));
    }

    @Override
    public SelectBuilder rightJoinOnEqual(String table, String key, String otherTable, String otherKey)
    {
        this.query.append(" \nRIGHT JOIN ").append(this.database.prepareTableName(table));
        this.onEqual(table, key, otherTable, otherKey);
        return this;
    }

    @Override
    public SelectBuilder joinOnEqual(String table, String key, String otherTable, String otherKey)
    {
        this.query.append(" JOIN ").append(this.database.prepareTableName(table));
        this.onEqual(table, key, otherTable, otherKey);
        return this;
    }
}
