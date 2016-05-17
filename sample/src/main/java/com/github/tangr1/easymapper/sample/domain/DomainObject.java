package com.github.tangr1.easymapper.sample.domain;

import java.io.Serializable;

public abstract class DomainObject implements Serializable {

    private Long id;

    public final Long getId() {
        return id;
    }

    public final void setId(Long id) {
        this.id = id;
    }
}
