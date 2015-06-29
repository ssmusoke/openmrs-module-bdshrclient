package org.openmrs.module.shrclient.model;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import java.util.Date;

public class IdMapping {

    private Logger logger = Logger.getLogger(IdMapping.class);

    private long id;
    private String internalId;
    private String externalId;
    private String type;
    private String uri;
    private Date lastSyncDateTime;

    public IdMapping(String internalId, String externalId, String type, String uri, Date lastSyncDateTime) {
        Validate.notNull(internalId);
        Validate.notNull(externalId);
        Validate.notNull(type);
        this.internalId = internalId;
        this.externalId = externalId;
        this.type = type;
        this.uri = uri;
        this.lastSyncDateTime = lastSyncDateTime;
    }

    public IdMapping(String internalId, String externalId, String type, String uri) {
        this(internalId, externalId, type, uri, null);
    }

    public IdMapping() {
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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Date getLastSyncDateTime() {
        return lastSyncDateTime;
    }

    public void setLastSyncDateTime(Date lastSyncDateTime) {
        this.lastSyncDateTime = lastSyncDateTime;
    }
}
