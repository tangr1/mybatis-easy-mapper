package com.github.tangr1.easymapper.mapper;

import com.github.tangr1.easymapper.mapper.internal.EntityColumn;
import com.github.tangr1.easymapper.mapper.internal.EntityTable;
import com.github.tangr1.easymapper.mapper.internal.SQLProvider;

import java.util.*;

public class Criteria {

    private final String clause;
    private final Class<?> entityClass;
    private final List<InternalCriteria> andList;
    private final List<String> orderBys;

    public Criteria(Builder criteriaBuilder) {
        this.entityClass = criteriaBuilder.entityClass;
        this.andList = criteriaBuilder.andList;
        this.orderBys = criteriaBuilder.orderBys;
        this.clause = criteriaBuilder.clause;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public List<InternalCriteria> getAndList() {
        return andList;
    }

    public List<String> getOrderBys() {
        return orderBys;
    }

    public String getClause() {
        return clause;
    }

    public enum Direction {
        ASC,
        DESC
    }

    public static class Builder {
        protected final List<InternalCriteria> andList;
        protected final Class<?> entityClass;
        protected final EntityTable entityTable;
        protected final Map<String, EntityColumn> propertyMap;
        protected final List<String> orderBys;
        protected List<Criterion> current;
        protected String clause = "";

        public Builder(Class<?> entityClass) {
            this.entityClass = entityClass;
            this.entityTable = SQLProvider.getEntityTable(entityClass);
            this.propertyMap = new HashMap<>(this.entityTable.getEntityClassColumns().size());
            for (EntityColumn column : entityTable.getEntityClassColumns()) {
                this.propertyMap.put(column.getProperty(), column);
            }
            this.orderBys = new ArrayList<>();
            this.andList = new ArrayList<>();
            List<Criterion> and = new ArrayList<>();
            andList.add(new InternalCriteria(and));
            current = and;
        }

        private String column(String property) {
            if (propertyMap.containsKey(property)) {
                return propertyMap.get(property).getColumn();
            } else {
                throw new RuntimeException(entityClass.getSimpleName() + " doesn't have the field " + property);
            }
        }

        public Builder isNull(String property) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " IS NULL"));
            return this;
        }

        public Builder isNotNull(String property) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " IS NOT NULL"));
            return this;
        }

        public Builder equalTo(String property, Object value) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " = ",
                    value));
            return this;
        }

        public Builder notEqualTo(String property, Object value) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " <> ",
                    value));
            return this;
        }

        public Builder greaterThan(String property, Object value) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " > ",
                    value));
            return this;
        }

        public Builder greaterThanOrEqualTo(String property, Object value) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " >= ",
                    value));
            return this;
        }

        public Builder lessThan(String property, Object value) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " < ",
                    value));
            return this;
        }

        public Builder lessThanOrEqualTo(String property, Object value) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " <= ",
                    value));
            return this;
        }

        public Builder in(String property, List<Object> values) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " in ",
                    values));
            return this;
        }

        public Builder notIn(String property, List<Object> values) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " NOT IN ",
                    values));
            return this;
        }

        public Builder between(String property, Object value1, Object value2) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " BETWEEN ",
                    value1, value2));
            return this;
        }

        public Builder notBetween(String property, Object value1, Object value2) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + " NOT BETWEEN ",
                    value1, value2));
            return this;
        }

        public Builder like(String property, String value) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + "  LIKE ",
                    value));
            return this;
        }

        public Builder notLike(String property, String value) {
            current.add(new Criterion(entityTable.getName() + "." + column(property) + "  NOT LIKE ",
                    value));
            return this;
        }

        public Builder or() {
            List<Criterion> and = new ArrayList<>();
            andList.add(new InternalCriteria(and));
            current = and;
            return this;
        }

        public Builder orderBy(String property, Direction direction) {
            orderBys.add(entityTable.getName() + "." + column(property) + " " + direction.toString());
            return this;
        }

        public Criteria build() {
            Criterion criterion;
            StringJoiner orJoiner = new StringJoiner(") OR (");
            for (int i = 0; i < andList.size(); i++) {
                StringJoiner andJoiner = new StringJoiner(" AND ");
                for (int j = 0; j < andList.get(i).getData().size(); j++) {
                    String subClause = "";
                    criterion = andList.get(i).getData().get(j);
                    if (criterion.isNoValue()) {
                        subClause += criterion.getCondition();
                    } else if (criterion.isSingleValue()) {
                        subClause += String.format("%s#{criteria.andList[%d].data[%d].value}",
                                criterion.getCondition(), i, j);
                    } else if (criterion.isBetweenValue()) {
                        subClause += String.format("%s#{criteria.andList[%d].data[%d].value} " +
                                        "AND #{criteria.andList[%d].data[%d].secondValue}",
                                criterion.getCondition(), i, j, i, j);
                    } else if (criterion.isListValue()) {
                        subClause += criterion.getCondition() + " (";
                        List<?> listItems = (List<?>) criterion.getValue();
                        StringJoiner sj = new StringJoiner(", ");
                        for (int k = 0; k < listItems.size(); k++) {
                            sj.add(String.format("#{criteria.andList[%d].data[%d].value[%d]}", i, j, k));
                        }
                        subClause += sj.toString() + ')';
                    }
                    if (!subClause.isEmpty()) {
                        andJoiner.add(subClause);
                    }
                }
                if (andJoiner.length() > 0) {
                    orJoiner.add(andJoiner.toString());
                }
            }
            if (orJoiner.length() > 0) {
                clause = "(" + orJoiner.toString() + ")";
            }
            return new Criteria(this);
        }
    }

    public static class InternalCriteria {
        public List<Criterion> getData() {
            return data;
        }

        private final List<Criterion> data;

        public InternalCriteria(List<Criterion> data) {
            this.data = data;
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        protected Criterion(String condition) {
            this.condition = condition;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value) {
            this.condition = condition;
            this.value = value;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.betweenValue = true;
        }

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }
    }
}