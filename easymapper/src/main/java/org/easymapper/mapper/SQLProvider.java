package org.easymapper.mapper;

import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.util.List;

public class SQLProvider {

    public String insert(Object record) {
        return new SQL() {{
            Class<?> entityClass = record.getClass();
            EntityTable entityTable = EntityHelper.getEntityTable(entityClass);
            MetaObject metaObject = SystemMetaObject.forObject(record);
            INSERT_INTO(entityTable.getName());
            entityTable.getEntityClassColumns().stream()
                    .filter(column -> metaObject.getValue(column.getProperty()) != null)
                    .forEach(column -> VALUES(column.getColumn(), "#{" + column.getProperty() + "}"));
        }}.toString();
    }

    public String count(Object record) {
        return new SQL() {{
            SELECT("SELECT COUNT(1)");
            FROM(EntityHelper.getEntityTable(record.getClass()).getName());
            applyWhere(record);
        }}.toString();
    }

    public String selectOne(Object record) {
        return new SQL() {{
            SELECT(EntityHelper.getAllColumns(record.getClass()));
            FROM(EntityHelper.getEntityTable(record.getClass()).getName());
            applyWhere(record);
        }}.toString() + " limit 1";
    }

    public String select(Object record) {
        return new SQL() {{
            SELECT(EntityHelper.getAllColumns(record.getClass()));
            FROM(EntityHelper.getEntityTable(record.getClass()).getName());
            applyWhere(record);
            String orderByClause = EntityHelper.getOrderByClause(record.getClass());
            if (orderByClause.length() > 0) {
                ORDER_BY(orderByClause);
            }
        }}.toString();
    }

    public String selectByExample(Example example) {
        return new SQL() {{
            Class<?> entityClass = example.getEntityClass();
            EntityTable entityTable = EntityHelper.getEntityTable(entityClass);
            SELECT(EntityHelper.getAllColumns(entityClass));
            FROM(entityTable.getName());
            MetaObject exampleMetaObject = SystemMetaObject.forObject(example);
            applyWhere(this, exampleMetaObject);
            applyOrderBy(this, exampleMetaObject, EntityHelper.getOrderByClause(entityClass));
        }}.toString();
    }

    public String updateByPrimaryKey(Object record) {
        return new SQL() {{
            Class<?> entityClass = record.getClass();
            EntityTable entityTable = EntityHelper.getEntityTable(entityClass);
            MetaObject metaObject = SystemMetaObject.forObject(record);
            UPDATE(entityTable.getName());
            entityTable.getEntityClassColumns().stream()
                    .filter(column -> !column.isId() && metaObject.getValue(column.getProperty()) != null)
                    .forEach(column -> SET(column.getColumn() + " = #{" + column.getProperty() + "}"));
            entityTable.getEntityClassPKColumns().forEach(column -> {
                notNullKeyProperty(column.getProperty(), metaObject.getValue(column.getProperty()));
                WHERE(column.getColumn() + "=#{" + column.getProperty() + "}");
            });
        }}.toString();
    }

    public String update(Object record, Object condition) {
        return new SQL() {{
            Class<?> entityClass = record.getClass();
            EntityTable entityTable = EntityHelper.getEntityTable(entityClass);
            UPDATE(entityTable.getName());
            MetaObject metaObject = SystemMetaObject.forObject(record);
            entityTable.getEntityClassColumns().stream()
                    .filter(column -> !column.isId() && metaObject.getValue(column.getProperty()) != null)
                    .forEach(column -> SET(column.getColumn() + " = #{record." + column.getProperty() + "}"));
            applyWhere(record);
        }}.toString();
    }

    public String updateByExample(Object record, Example example) {
        return new SQL() {{
            Class<?> entityClass = record.getClass();
            EntityTable entityTable = EntityHelper.getEntityTable(entityClass);
            MetaObject metaObject = SystemMetaObject.forObject(record);
            UPDATE(entityTable.getName());
            entityTable.getEntityClassColumns().stream()
                    .filter(column -> !column.isId() && metaObject.getValue(column.getProperty()) != null)
                    .forEach(column -> SET(column.getColumn() + " = #{record." + column.getProperty() + "}"));
            MetaObject exampleMetaObject = SystemMetaObject.forObject(example);
            applyWhere(this, exampleMetaObject, "example");
        }}.toString();
    }

    public String delete(Object record) {
        return new SQL() {{
            DELETE_FROM(EntityHelper.getEntityTable(record.getClass()).getName());
            applyWhere(record);
        }}.toString();
    }

    public String deleteByExample(Example example) {
        return new SQL() {{
            Class<?> entityClass = example.getEntityClass();
            EntityTable entityTable = EntityHelper.getEntityTable(entityClass);
            SELECT(EntityHelper.getAllColumns(entityClass));
            DELETE_FROM(entityTable.getName());
            MetaObject exampleMetaObject = SystemMetaObject.forObject(example);
            applyWhere(this, exampleMetaObject);
        }}.toString();
    }

    protected SQL applyWhere(Object record) {
        return new SQL() {{
            Class<?> entityClass = record.getClass();
            EntityTable entityTable = EntityHelper.getEntityTable(entityClass);
            MetaObject metaObject = SystemMetaObject.forObject(record);
            entityTable.getEntityClassColumns().stream()
                    .filter(column -> {
                        Object value = metaObject.getValue(column.getProperty());
                        return value != null
                                && !(column.getJavaType().equals(String.class) && ((String) value).length() == 0);
                    })
                    .forEach(column -> WHERE(entityTable.getName() +
                            "." + column.getColumn() + " = #{" + column.getProperty() + "}"));
        }};
    }


    protected void notNullKeyProperty(String property, Object value) {
        if (value == null || (value instanceof String && ((String) value).length() == 0)) {
            throw new NullPointerException("主键属性" + property + "不能为空!");
        }
    }

    protected void applyWhere(SQL sql, MetaObject example) {
        applyWhere(sql, example, null);
    }

    protected void applyWhere(SQL sql, MetaObject example, String prefix) {
        if (example == null) {
            return;
        }
        String parmPhrase1;
        String parmPhrase1_th;
        String parmPhrase2;
        String parmPhrase2_th;
        String parmPhrase3;
        String parmPhrase3_th;
        if (prefix == null) {
            parmPhrase1 = "%s #{oredCriteria[%d].allCriteria[%d].value}";
            parmPhrase1_th = "%s #{oredCriteria[%d].allCriteria[%d].value,typeHandler=%s}";
            parmPhrase2 = "%s #{oredCriteria[%d].allCriteria[%d].value} and #{oredCriteria[%d].criteria[%d].secondValue}";
            parmPhrase2_th = "%s #{oredCriteria[%d].allCriteria[%d].value,typeHandler=%s} and #{oredCriteria[%d].criteria[%d].secondValue,typeHandler=%s}";
            parmPhrase3 = "#{oredCriteria[%d].allCriteria[%d].value[%d]}";
            parmPhrase3_th = "#{oredCriteria[%d].allCriteria[%d].value[%d],typeHandler=%s}";
        } else {
            parmPhrase1 = "%s #{example.oredCriteria[%d].allCriteria[%d].value}";
            parmPhrase1_th = "%s #{example.oredCriteria[%d].allCriteria[%d].value,typeHandler=%s}";
            parmPhrase2 = "%s #{example.oredCriteria[%d].allCriteria[%d].value} and #{example.oredCriteria[%d].criteria[%d].secondValue}";
            parmPhrase2_th = "%s #{example.oredCriteria[%d].allCriteria[%d].value,typeHandler=%s} and #{example.oredCriteria[%d].criteria[%d].secondValue,typeHandler=%s}";
            parmPhrase3 = "#{example.oredCriteria[%d].allCriteria[%d].value[%d]}";
            parmPhrase3_th = "#{example.oredCriteria[%d].allCriteria[%d].value[%d],typeHandler=%s}";
        }

        StringBuilder sb = new StringBuilder();

        List<?> oredCriteria = (List<?>) example.getValue("oredCriteria");
        boolean firstCriteria = true;
        for (int i = 0; i < oredCriteria.size(); i++) {
            MetaObject criteria = SystemMetaObject.forObject(oredCriteria.get(i));
            List<?> criterions = (List<?>) criteria.getValue("criteria");
            if (criterions.size() > 0) {
                if (firstCriteria) {
                    firstCriteria = false;
                } else {
                    sb.append(" or ");
                }

                sb.append('(');
                boolean firstCriterion = true;
                for (int j = 0; j < criterions.size(); j++) {
                    MetaObject criterion = SystemMetaObject.forObject(criterions.get(j));
                    if (firstCriterion) {
                        firstCriterion = false;
                    } else {
                        sb.append(" and ");
                    }

                    if ((Boolean) criterion.getValue("noValue")) {
                        sb.append(criterion.getValue("condition"));
                    } else if ((Boolean) criterion.getValue("singleValue")) {
                        if (criterion.getValue("typeHandler") == null) {
                            sb.append(String.format(parmPhrase1, criterion.getValue("condition"), i, j));
                        } else {
                            sb.append(String.format(parmPhrase1_th, criterion.getValue("condition"), i, j, criterion.getValue("typeHandler")));
                        }
                    } else if ((Boolean) criterion.getValue("betweenValue")) {
                        if (criterion.getValue("typeHandler") == null) {
                            sb.append(String.format(parmPhrase2, criterion.getValue("condition"), i, j, i, j));
                        } else {
                            sb.append(String.format(parmPhrase2_th, criterion.getValue("condition"), i, j, criterion.getValue("typeHandler"), i, j, criterion.getValue("typeHandler")));
                        }
                    } else if ((Boolean) criterion.getValue("listValue")) {
                        sb.append(criterion.getValue("condition"));
                        sb.append(" (");
                        List<?> listItems = (List<?>) criterion.getValue("value");
                        boolean comma = false;
                        for (int k = 0; k < listItems.size(); k++) {
                            if (comma) {
                                sb.append(", ");
                            } else {
                                comma = true;
                            }
                            if (criterion.getValue("typeHandler") == null) {
                                sb.append(String.format(parmPhrase3, i, j, k));
                            } else {
                                sb.append(String.format(parmPhrase3_th, i, j, k, criterion.getValue("typeHandler")));
                            }
                        }
                        sb.append(')');
                    }
                }
                sb.append(')');
            }
        }
        if (sb.length() > 0) {
            sql.WHERE(sb.toString());
        }
    }

    protected void applyOrderBy(SQL sql, MetaObject example, String defaultOrderByClause) {
        if (example == null) {
            return;
        }
        Object orderBy = example.getValue("orderByClause");
        if (orderBy != null) {
            sql.ORDER_BY((String) orderBy);
        } else if (defaultOrderByClause != null && defaultOrderByClause.length() > 0) {
            sql.ORDER_BY(defaultOrderByClause);
        }
    }
}
