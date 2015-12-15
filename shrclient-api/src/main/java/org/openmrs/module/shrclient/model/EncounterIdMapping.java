package org.openmrs.module.shrclient.model;

import java.util.Date;

public class EncounterIdMapping extends IdMapping{


    public EncounterIdMapping(String internalId, String externalId, String uri, Date lastSyncDateTime, Date serverUpdateDateTime) {
        super(internalId,externalId, IdMappingType.ENCOUNTER,uri,lastSyncDateTime, serverUpdateDateTime);
    }

    public EncounterIdMapping(String internalId, String externalId, String uri, Date lastSyncDateTime) {
        this(internalId, externalId, uri, lastSyncDateTime, null);
    }

}
