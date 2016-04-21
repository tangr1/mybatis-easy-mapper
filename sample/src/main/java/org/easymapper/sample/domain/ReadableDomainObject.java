package org.easymapper.sample.domain;

import org.easymapper.annotation.CreatedAt;

import java.util.Date;

public abstract class ReadableDomainObject extends DomainObject {

    @CreatedAt
    private Date createdAt;

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
