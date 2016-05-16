package org.easymapper.mapper.internal;

public class EntityColumn {
    private String property;
    private String column;
    private Class<?> javaType;
    private String orderBy;
    private boolean id = false;
    private boolean createdAt = false;
    private boolean updatedAt = false;

    public boolean isCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(boolean createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(boolean updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public void setJavaType(Class<?> javaType) {
        this.javaType = javaType;
    }

    public boolean isId() {
        return id;
    }

    public void setId(boolean id) {
        this.id = id;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }
}
