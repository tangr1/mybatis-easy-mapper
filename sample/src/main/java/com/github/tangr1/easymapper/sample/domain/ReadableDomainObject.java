package com.github.tangr1.easymapper.sample.domain;

import com.github.tangr1.easymapper.annotation.CreatedAt;

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
