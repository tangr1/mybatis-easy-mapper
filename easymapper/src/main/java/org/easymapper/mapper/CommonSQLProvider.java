package org.easymapper.mapper;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.jdbc.SQL;
import org.easymapper.annotation.CreatedAt;
import org.easymapper.annotation.Join;
import org.easymapper.annotation.ProviderConfig;
import org.easymapper.annotation.UpdatedAt;

import javax.persistence.Table;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.util.*;

public class CommonSQLProvider {
    protected Map<String, String> mappings = new HashMap<>();
    protected String readColumns = null;
    protected Map<String, String> selectColumns = new HashMap<>();
    protected String tableName = null;
    protected List<String> joinColumns = new ArrayList<>();
    protected List<String> innerJoins = new ArrayList<>();
    protected String defaultOrderColumn = "id";
    protected String defaultOrderDirection = "desc";

    public CommonSQLProvider() {
        Class<?> clazz = this.getClass().getAnnotation(ProviderConfig.class).entity();
        tableName = getTableName(clazz);
        for (Field field : FieldUtils.getAllFieldsList(clazz)) {
            if (field.getAnnotation(Transient.class) == null) {
                String columnName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());
                mappings.put(columnName, "#{" + field.getName() + "}");
                switch (field.getGenericType().getTypeName()) {
                    case "int":
                    case "long":
                    case "java.lang.Integer":
                    case "java.lang.Long":
                        selectColumns.put(columnName, " = %s");
                        break;
                    case "java.lang.String":
                        selectColumns.put(columnName, " like '%s%%'");
                        break;
                    case "java.util.Date":
                        selectColumns.put(columnName, " %s");
                        break;
                    default:
                        break;
                }
            } else if (field.getAnnotation(Join.class) != null) {
                Join join = field.getAnnotation(Join.class);
                String foreignTableName = getTableName(join.entity());
                joinColumns.add(foreignTableName + "." +
                        CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, join.field()) +
                        " as " +
                        CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName()));
                innerJoins.add(foreignTableName + " on " + tableName + "." +
                        CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, join.joinField()) +
                        " = " + foreignTableName + "." +
                        CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, join.foreignJoinField()));
            }
        }
        readColumns = tableName + ".id, " + tableName + "." + Joiner.on(", " + tableName + ".").join(mappings.keySet());
        if (!joinColumns.isEmpty()) {
            readColumns += ", " + Joiner.on(", ").join(joinColumns);
        }
    }

    private String getTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, clazz.getSimpleName());
        } else {
            return table.name();
        }
    }

    public String findByIds(Map<String, Object> params) {
        final List<Integer> ids = (List<Integer>) params.get("ids");
        SQL sql = new SQL();
        sql.SELECT(readColumns);
        sql.FROM(tableName);
        for (String innerJoin : innerJoins) {
            sql.INNER_JOIN(innerJoin);
        }
        if (ids == null || ids.isEmpty()) {
            sql.WHERE(tableName + ".id in (0)");
        } else {
            sql.WHERE(tableName + ".id in (" + Joiner.on(", ").join(ids) + ")");
        }
        return sql.toString();
    }

    protected SQL selectMultiFields(Map<String, Object> select, SQL sql) {
        for (Map.Entry<String, String> entry : selectColumns.entrySet()) {
            String key = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, entry.getKey());
            if (select.containsKey(key)) {
                Object value = select.get(key);
                if (value instanceof String) {
                    sql.WHERE(tableName + "." + entry.getKey() + String.format(entry.getValue(), value));
                } else if (value instanceof List) {
                    if (!((List) value).isEmpty()) {
                        String statement = "";
                        for (String s : (List<String>) value) {
                            statement += " OR ";
                            statement += tableName + "." + entry.getKey() + String.format(entry.getValue(), s);
                        }
                        statement += ")";
                        statement = statement.replaceFirst(" OR ", "(");
                        sql.WHERE(statement);
                    }
                }
            }
        }
        return sql;
    }

    public String findByMultiFields(Map<String, Object> params) {
        String sort = null;
        if (params.containsKey("sort")) {
            sort = (String) params.get("sort");
        }
        String direction = null;
        if (params.containsKey("direction")) {
            direction = (String) params.get("direction");
        }
        final Map<String, Object> select = (Map<String, Object>) params.get("select");
        SQL sql = new SQL();
        sql.SELECT(readColumns);
        sql.FROM(tableName);
        for (String innerJoin : innerJoins) {
            sql.INNER_JOIN(innerJoin);
        }
        sql = selectMultiFields(select, sql);
        if (sort != null) {
            sql.ORDER_BY(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sort) + " " + direction);
        } else {
            if (mappings.containsKey(defaultOrderColumn)) {
                sql.ORDER_BY(tableName + "." + defaultOrderColumn + " " + defaultOrderDirection);
            }
        }
        return sql.toString();
    }

    public String countByMultiFields(Map<String, Object> params) {
        final Map<String, Object> select = (Map<String, Object>) params.get("select");
        SQL sql = new SQL();
        sql.SELECT("count(id)");
        sql.FROM(tableName);
        sql = selectMultiFields(select, sql);
        return sql.toString();
    }

    public String create(Object value) {
        StringJoiner columns = new StringJoiner(",");
        StringJoiner values = new StringJoiner(",");
        for (String column : mappings.keySet()) {
            try {
                Field field = FieldUtils.getField(value.getClass(),
                        CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, column), true);
                if (field.get(value) != null && field.getAnnotation(Transient.class) == null) {
                    columns.add(column);
                    values.add(mappings.get(column));
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return new SQL() {{
            INSERT_INTO(tableName);
            VALUES(columns.toString(), values.toString());
        }}.toString();
    }

    public String update(Object value) {
        SQL sql = new SQL();
        sql.UPDATE(tableName);
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            try {
                Field field = FieldUtils.getField(value.getClass(),
                        CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, entry.getKey()), true);
                if (field.getAnnotation(UpdatedAt.class) != null) {
                    sql.SET(entry.getKey() + " = now()");
                } else if (field.get(value) != null
                        && field.getAnnotation(Transient.class) == null
                        && field.getAnnotation(CreatedAt.class) == null) {
                    sql.SET(entry.getKey() + " = " + entry.getValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Ignore
            }
        }
        sql.WHERE("id = #{id}");
        return sql.toString();
    }

    public String findById() {
        SQL sql = new SQL();
        sql.SELECT(readColumns);
        sql.FROM(tableName);
        for (String innerJoin : innerJoins) {
            sql.INNER_JOIN(innerJoin);
        }
        sql.WHERE(tableName + ".id = #{id}");
        return sql.toString();
    }

    public String findAll() {
        return new SQL() {{
            SELECT(readColumns);
            FROM(tableName);
            for (String innerJoin : innerJoins) {
                INNER_JOIN(innerJoin);
            }
            if (mappings.containsKey(defaultOrderColumn)) {
                ORDER_BY(tableName + "." + defaultOrderColumn + " " + defaultOrderDirection);
            }
        }}.toString();
    }

    public String countAll() {
        return new SQL() {{
            SELECT("count(id)");
            FROM(tableName);
        }}.toString();
    }

    public String deleteById() {
        return new SQL() {{
            DELETE_FROM(tableName);
            WHERE("id = #{id}");
        }}.toString();
    }
}
