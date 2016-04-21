package org.easymapper.sample.domain;

import org.easymapper.annotation.UpdatedAt;

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
