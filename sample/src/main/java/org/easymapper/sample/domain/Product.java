package org.easymapper.sample.domain;

import org.easymapper.annotation.Join;

import javax.persistence.Transient;

public class Product extends ModifiableDomainObject {

    private Long companyId;
    private String name;
    private String description;
    private Integer status;
    @Transient
    @Join(entity = Company.class, field = "name", joinField = "companyId", foreignJoinField = "id")
    private String companyName;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
