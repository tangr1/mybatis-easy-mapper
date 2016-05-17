package com.github.tangr1.easymapper.mapper.internal;

import java.util.List;
import java.util.Set;

public class EntityTable {
    private String name;
    private Set<EntityColumn> entityClassColumns;
    private Set<EntityColumn> entityClassPKColumns;
    private String selectColumns;
    private List<String> innerJoins;
    private List<String> orderBys;

    public List<String> getInnerJoins() {
        return innerJoins;
    }

    public void setInnerJoins(List<String> innerJoins) {
        this.innerJoins = innerJoins;
    }

    public List<String> getOrderBys() {
        return orderBys;
    }

    public void setOrderBys(List<String> orderBys) {
        this.orderBys = orderBys;
    }

    public String getSelectColumns() {
        return selectColumns;
    }

    public void setSelectColumns(String selectColumns) {
        this.selectColumns = selectColumns;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<EntityColumn> getEntityClassColumns() {
        return entityClassColumns;
    }

    public void setEntityClassColumns(Set<EntityColumn> entityClassColumns) {
        this.entityClassColumns = entityClassColumns;
    }

    public Set<EntityColumn> getEntityClassPKColumns() {
        return entityClassPKColumns;
    }

    public void setEntityClassPKColumns(Set<EntityColumn> entityClassPKColumns) {
        this.entityClassPKColumns = entityClassPKColumns;
    }
}
