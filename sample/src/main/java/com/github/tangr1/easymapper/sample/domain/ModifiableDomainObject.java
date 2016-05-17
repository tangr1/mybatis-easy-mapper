package com.github.tangr1.easymapper.sample.domain;

import com.github.tangr1.easymapper.annotation.UpdatedAt;

import java.util.Date;

public abstract class ModifiableDomainObject extends ReadableDomainObject {

    @UpdatedAt
    private Date updatedAt;

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
