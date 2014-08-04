package org.openmrs.module.shrclient.model;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

public class IdMapping {

    private Logger logger = Logger.getLogger(IdMapping.class);

    private long id;
    private String internalId;
    private String externalId;
    private String type;
    private String url;

    public IdMapping(String internalId, String externalId, String type, String url) {
        Validate.notNull(internalId);
        Validate.notNull(externalId);
        Validate.notNull(type);
        Validate.notNull(url);
        this.internalId = internalId;
        this.externalId = externalId;
        this.type = type;
        this.url = url;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
