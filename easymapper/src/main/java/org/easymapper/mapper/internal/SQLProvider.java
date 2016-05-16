package org.easymapper.mapper.internal;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.easymapper.annotation.CreatedAt;
import org.easymapper.annotation.Reference;
import org.easymapper.annotation.UpdatedAt;
import org.easymapper.mapper.Criteria;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.util.*;

public class SQLProvider {

    protected static final Map<Class<?>, EntityTable> entityTableMap = new HashMap<>();

    public static EntityTable getEntityTable(Class<?> entityClass) {
        EntityTable entityTable = entityTableMap.get(entityClass);
        if (entityTable == null) {
            initEntityNameMap(entityClass);
            entityTable = entityTableMap.get(entityClass);
        }
        if (entityTable == null) {
            throw new RuntimeException("Cannot get table info of " + entityClass.getCanonicalName());
        }
        return entityTable;
    }

    protected static EntityColumn getColumnFromField(Field field) {
        EntityColumn entityColumn = new EntityColumn();
        if (field.isAnnotationPresent(Id.class)) {
            entityColumn.setId(true);
        }
        if (field.isAnnotationPresent(CreatedAt.class)) {
            entityColumn.setCreatedAt(true);
        }
        if (field.isAnnotationPresent(UpdatedAt.class)) {
            entityColumn.setUpdatedAt(true);
        }
        String columnName = null;
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            columnName = column.name();
        }
        if (columnName == null || columnName.equals("")) {
            columnName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());
        }
        entityColumn.setProperty(field.getName());
        entityColumn.setColumn(columnName);
        entityColumn.setJavaType(field.getType());
        if (field.isAnnotationPresent(OrderBy.class)) {
            OrderBy orderBy = field.getAnnotation(OrderBy.class);
            if (orderBy.value().equals("")) {
                entityColumn.setOrderBy("ASC");
            } else {
                entityColumn.setOrderBy(orderBy.value());
            }
        }
        return entityColumn;
    }

    protected static String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            if (!table.name().isEmpty()) {
                return table.name();
            }
        }
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entityClass.getSimpleName());
    }

    protected static synchronized void initEntityNameMap(Class<?> entityClass) {
        if (entityTableMap.get(entityClass) != null) {
            return;
        }
        EntityTable entityTable = new EntityTable();
        entityTable.setName(getTableName(entityClass));

        List<Field> fieldList = FieldUtils.getAllFieldsList(entityClass);
        Set<EntityColumn> columnSet = new LinkedHashSet<>();
        Set<EntityColumn> pkColumnSet = new LinkedHashSet<>();
        Map<String, Reference> referenceMap = new HashMap<>();
        List<String> innerJoins = new ArrayList<>();
        List<String> orderBys = new ArrayList<>();
        for (Field field : fieldList) {
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }
            if (field.isAnnotationPresent(Reference.class)) {
                referenceMap.put(field.getName(), field.getAnnotation(Reference.class));
                continue;
            }
            EntityColumn entityColumn = getColumnFromField(field);
            columnSet.add(entityColumn);
            if (entityColumn.isId()) {
                pkColumnSet.add(entityColumn);
            }
        }
        entityTable.setEntityClassColumns(columnSet);

        if (pkColumnSet.isEmpty()) {
            fieldList.stream()
                    .filter(field -> !field.isAnnotationPresent(Transient.class)
                            && !field.isAnnotationPresent(Reference.class)
                            && field.getName().equalsIgnoreCase("id"))
                    .forEach(field -> pkColumnSet.add(getColumnFromField(field)));
        }
        entityTable.setEntityClassPKColumns(pkColumnSet);

        StringJoiner stringJoiner = new StringJoiner(", ");
        columnSet.forEach(column -> stringJoiner.add(entityTable.getName() + "." + column.getColumn()));
        referenceMap.entrySet().forEach(entry -> {
            String foreignTableName = getTableName(entry.getValue().referenceEntity());
            stringJoiner.add(foreignTableName + "." +
                    CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getValue().referenceField()) +
                    " as " +
                    CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getKey()));
            innerJoins.add(foreignTableName + " on " + entityTable.getName() + "." +
                    CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getValue().localOn()) +
                    " = " + foreignTableName + "." +
                    CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getValue().referenceOn()));
        });
        entityTable.setSelectColumns(stringJoiner.toString());
        entityTable.setInnerJoins(innerJoins);

        columnSet.stream()
                .filter(column -> column.getOrderBy() != null)
                .forEach(column -> orderBys.add(column.getColumn() + " " + column.getOrderBy()));
        entityTable.setOrderBys(orderBys);

        entityTableMap.put(entityClass, entityTable);
    }

    public String insert(Object record) {
        EntityTable entityTable = getEntityTable(record.getClass());
        MetaObject metaObject = SystemMetaObject.forObject(record);
        return new SQL() {{
            INSERT_INTO(entityTable.getName());
            entityTable.getEntityClassColumns().stream()
                    .filter(column -> metaObject.getValue(column.getProperty()) != null)
                    .forEach(column -> VALUES(column.getColumn(), "#{" + column.getProperty() + "}"));
        }}.toString();
    }

    public String count(Object record) {
        EntityTable entityTable = getEntityTable(record.getClass());
        return new SQL() {{
            SELECT("SELECT COUNT(1)");
            FROM(entityTable.getName());
            applyWhereFromRecord(this, entityTable, record);
        }}.toString();
    }

    public String selectOne(Object record) {
        EntityTable entityTable = getEntityTable(record.getClass());
        return new SQL() {{
            SELECT(entityTable.getSelectColumns());
            FROM(entityTable.getName());
            entityTable.getInnerJoins().forEach(this::INNER_JOIN);
            applyWhereFromRecord(this, entityTable, record);
        }}.toString() + " limit 1";
    }

    public String select(Object record) {
        EntityTable entityTable = getEntityTable(record.getClass());
        return new SQL() {{
            SELECT(entityTable.getSelectColumns());
            FROM(entityTable.getName());
            entityTable.getInnerJoins().forEach(this::INNER_JOIN);
            applyWhereFromRecord(this, entityTable, record);
            entityTable.getOrderBys().forEach(this::ORDER_BY);
        }}.toString();
    }

    public String selectByCriteria(Criteria criteria) {
        EntityTable entityTable = getEntityTable(criteria.getEntityClass());
        return new SQL() {{
            SELECT(entityTable.getSelectColumns());
            FROM(entityTable.getName());
            entityTable.getInnerJoins().forEach(this::INNER_JOIN);
            WHERE(criteria.getClause());
            StringJoiner stringJoiner = new StringJoiner(", ");
            if (criteria.getOrderBys() != null && !criteria.getOrderBys().isEmpty()) {
                criteria.getOrderBys().forEach(stringJoiner::add);
            } else if (entityTable.getOrderBys() != null && !entityTable.getOrderBys().isEmpty()) {
                entityTable.getOrderBys().forEach(stringJoiner::add);
            }
            if (stringJoiner.length() > 0) {
                ORDER_BY(stringJoiner.toString());
            }
        }}.toString();
    }

    public String updateByPrimaryKey(Object record) {
        EntityTable entityTable = getEntityTable(record.getClass());
        MetaObject metaObject = SystemMetaObject.forObject(record);
        return new SQL() {{
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
        EntityTable entityTable = getEntityTable(record.getClass());
        MetaObject metaObject = SystemMetaObject.forObject(record);
        return new SQL() {{
            UPDATE(entityTable.getName());
            applyUpdate(this, entityTable, metaObject);
            applyWhereFromRecord(this, entityTable, condition, "condition.");
        }}.toString();
    }

    public String updateByCriteria(Object record, Criteria criteria) {
        EntityTable entityTable = getEntityTable(criteria.getEntityClass());
        MetaObject metaObject = SystemMetaObject.forObject(record);
        MetaObject criteriaMetaObject = SystemMetaObject.forObject(criteria);
        return new SQL() {{
            UPDATE(entityTable.getName());
            applyUpdate(this, entityTable, metaObject);
            WHERE(criteria.getClause());
        }}.toString();
    }

    public String delete(Object record) {
        EntityTable entityTable = getEntityTable(record.getClass());
        return new SQL() {{
            DELETE_FROM(entityTable.getName());
            applyWhereFromRecord(this, entityTable, record);
        }}.toString();
    }

    public String deleteByCriteria(Criteria criteria) {
        EntityTable entityTable = getEntityTable(criteria.getEntityClass());
        return new SQL() {{
            DELETE_FROM(entityTable.getName());
            WHERE(criteria.getClause());
        }}.toString();
    }

    protected void applyUpdate(SQL sql, EntityTable entityTable, MetaObject metaObject) {
        entityTable.getEntityClassColumns().stream()
                .filter(column -> !column.isId() && !column.isCreatedAt() && metaObject.getValue(column.getProperty()) != null)
                .forEach(column -> {
                    if (column.isUpdatedAt()) {
                        sql.SET(column.getColumn() + " = now()");
                    } else {
                        sql.SET(column.getColumn() + " = #{record." + column.getProperty() + "}");
                    }
                });
    }

    protected void applyWhereFromRecord(SQL sql, EntityTable entityTable, Object condition) {
        applyWhereFromRecord(sql, entityTable, condition, "");
    }

    protected void applyWhereFromRecord(SQL sql, EntityTable entityTable, Object condition, String prefix) {
        MetaObject metaObject = SystemMetaObject.forObject(condition);
        entityTable.getEntityClassColumns().stream()
                .filter(column -> {
                    Object value = metaObject.getValue(column.getProperty());
                    return value != null
                            && !(column.getJavaType().equals(String.class) && ((String) value).length() == 0);
                })
                .forEach(column -> sql.WHERE(entityTable.getName() +
                        "." + column.getColumn() + " = #{" + prefix + column.getProperty() + "}")
                );
    }

    protected void notNullKeyProperty(String property, Object value) {
        if (value == null || (value instanceof String && ((String) value).length() == 0)) {
            throw new NullPointerException("主键属性" + property + "不能为空!");
        }
    }
}
