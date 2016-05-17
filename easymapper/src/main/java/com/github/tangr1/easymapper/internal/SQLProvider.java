package com.github.tangr1.easymapper.internal;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import com.github.tangr1.easymapper.annotation.CreatedAt;
import com.github.tangr1.easymapper.annotation.Reference;
import com.github.tangr1.easymapper.annotation.UpdatedAt;
import com.github.tangr1.easymapper.Criteria;
import org.springframework.data.domain.Pageable;

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

    public String count(Map<String, Object> params) {
        Object condition = params.get("condition");
        EntityTable entityTable = getEntityTable(condition.getClass());
        return new SQL() {{
            SELECT("COUNT(1)");
            FROM(entityTable.getName());
            applyWhere(this, entityTable, condition);
        }}.toString();
    }

    public String selectOne(Map<String, Object> params) {
        Object condition = params.get("condition");
        EntityTable entityTable = getEntityTable(condition.getClass());
        return new SQL() {{
            SELECT(entityTable.getSelectColumns());
            FROM(entityTable.getName());
            entityTable.getInnerJoins().forEach(this::INNER_JOIN);
            applyWhere(this, entityTable, condition);
        }}.toString() + " LIMIT 1";
    }

    public String select(Map<String, Object> params) {
        Object condition = params.get("condition");
        EntityTable entityTable = getEntityTable(condition.getClass());
        Pageable pageable = null;
        if (params.containsKey("pageable")) {
            pageable = (Pageable) params.get("pageable");
        }
        SQL sql = new SQL();
        sql.SELECT(entityTable.getSelectColumns());
        sql.FROM(entityTable.getName());
        entityTable.getInnerJoins().forEach(sql::INNER_JOIN);
        applyWhere(sql, entityTable, condition);
        if (pageable == null) {
            entityTable.getOrderBys().forEach(sql::ORDER_BY);
            return sql.toString();
        } else {
            return applyPageable(sql, pageable);
        }
    }

    public String selectByCriteria(Map<String, Object> params) {
        Criteria criteria = (Criteria) params.get("criteria");
        Pageable pageable = null;
        if (params.containsKey("pageable")) {
            pageable = (Pageable) params.get("pageable");
        }
        EntityTable entityTable = getEntityTable(criteria.getEntityClass());
        SQL sql = new SQL();
        sql.SELECT(entityTable.getSelectColumns());
        sql.FROM(entityTable.getName());
        entityTable.getInnerJoins().forEach(sql::INNER_JOIN);
        sql.WHERE(criteria.getClause());
        StringJoiner stringJoiner = new StringJoiner(", ");
        if (criteria.getOrderBys() != null && !criteria.getOrderBys().isEmpty()) {
            criteria.getOrderBys().forEach(stringJoiner::add);
        } else if (entityTable.getOrderBys() != null && !entityTable.getOrderBys().isEmpty()) {
            entityTable.getOrderBys().forEach(stringJoiner::add);
        }
        if (stringJoiner.length() > 0) {
            sql.ORDER_BY(stringJoiner.toString());
        }
        if (pageable == null) {
            entityTable.getOrderBys().forEach(sql::ORDER_BY);
            return sql.toString();
        } else {
            return applyPageable(sql, pageable);
        }
    }

    public String countByCriteria(Criteria criteria) {
        EntityTable entityTable = getEntityTable(criteria.getEntityClass());
        return new SQL() {{
            SELECT("COUNT(1)");
            FROM(entityTable.getName());
            entityTable.getInnerJoins().forEach(this::INNER_JOIN);
            WHERE(criteria.getClause());
        }}.toString();
    }

    public String updateByPrimaryKey(Map<String, Object> params) {
        Object record = params.get("record");
        EntityTable entityTable = getEntityTable(record.getClass());
        MetaObject metaObject = SystemMetaObject.forObject(record);
        return new SQL() {{
            UPDATE(entityTable.getName());
            entityTable.getEntityClassColumns().stream()
                    .filter(column -> !column.isId() && metaObject.getValue(column.getProperty()) != null)
                    .forEach(column -> SET(column.getColumn() + " = #{record." + column.getProperty() + "}"));
            entityTable.getEntityClassPKColumns().forEach(column -> {
                notNullKeyProperty(column.getProperty(), metaObject.getValue(column.getProperty()));
                WHERE(column.getColumn() + " = #{record." + column.getProperty() + "}");
            });
        }}.toString();
    }

    public String update(Object record, Object condition) {
        EntityTable entityTable = getEntityTable(record.getClass());
        MetaObject metaObject = SystemMetaObject.forObject(record);
        return new SQL() {{
            UPDATE(entityTable.getName());
            applyUpdate(this, entityTable, metaObject);
            applyWhere(this, entityTable, condition);
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

    public String delete(Map<String, Object> params) {
        Object condition = params.get("condition");
        EntityTable entityTable = getEntityTable(condition.getClass());
        return new SQL() {{
            DELETE_FROM(entityTable.getName());
            applyWhere(this, entityTable, condition);
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
                        sql.SET(column.getColumn() + " = NOW()");
                    } else {
                        sql.SET(column.getColumn() + " = #{record." + column.getProperty() + "}");
                    }
                });
    }

    protected void applyWhere(SQL sql, EntityTable entityTable, Object condition) {
        MetaObject metaObject = SystemMetaObject.forObject(condition);
        entityTable.getEntityClassColumns().stream()
                .filter(column -> {
                    Object value = metaObject.getValue(column.getProperty());
                    return value != null
                            && !(column.getJavaType().equals(String.class) && ((String) value).length() == 0);
                })
                .forEach(column -> sql.WHERE(entityTable.getName() +
                        "." + column.getColumn() + " = #{condition." + column.getProperty() + "}")
                );
    }

    protected String applyPageable(SQL sql, Pageable pageable) {
        StringJoiner stringJoiner = new StringJoiner(", ");
        pageable.getSort().forEach(order -> stringJoiner.add(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE
                , order.getProperty()) + " " + order.getDirection().name()));
        if (stringJoiner.length() > 0) {
            sql.ORDER_BY(stringJoiner.toString());
        }
        return sql.toString() + " LIMIT " + pageable.getOffset() + ", " + pageable.getPageSize();
    }

    protected void notNullKeyProperty(String property, Object value) {
        if (value == null || (value instanceof String && ((String) value).length() == 0)) {
            throw new NullPointerException("Primary key " + property + " cannot be empty!");
        }
    }
}
