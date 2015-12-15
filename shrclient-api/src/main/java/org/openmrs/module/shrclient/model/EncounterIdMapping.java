package org.openmrs.module.shrclient.model;

import java.util.Date;

public class EncounterIdMapping extends IdMapping{


    private Date serverUpdateDateTime;

    public EncounterIdMapping(String internalId, String externalId, String uri, Date lastSyncDateTime, Date serverUpdateDateTime) {

        super(internalId,externalId, IdMappingType.ENCOUNTER,uri,lastSyncDateTime);
        this.serverUpdateDateTime = serverUpdateDateTime;
    }

    public EncounterIdMapping(String internalId, String externalId, String uri, Date lastSyncDateTime) {
        this(internalId, externalId, uri, lastSyncDateTime, null);
    }

    public EncounterIdMapping(String internalId, String externalId, String uri) {
        this(internalId, externalId, uri, null, null);
    }

    public EncounterIdMapping() {
    }

    public Date getServerUpdateDateTime() {
        return serverUpdateDateTime;
    }

    public void setServerUpdateDateTime(Date serverUpdateDateTime) {
        this.serverUpdateDateTime = serverUpdateDateTime;
    }
}
