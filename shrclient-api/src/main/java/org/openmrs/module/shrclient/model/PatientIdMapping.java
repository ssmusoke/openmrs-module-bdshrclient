package org.openmrs.module.shrclient.model;

import java.util.Date;

public class PatientIdMapping extends IdMapping {

    public PatientIdMapping(String internalId, String externalId, String uri, Date lastSyncDateTime, Date serverUpdateDateTime) {
        super(internalId, externalId, IdMappingType.PATIENT, uri, lastSyncDateTime, serverUpdateDateTime);
    }

    public PatientIdMapping(String internalId, String externalId, String uri, Date lastSyncDateTime) {
        this(internalId, externalId, uri, lastSyncDateTime, null);
    }

    public PatientIdMapping(String internalId, String externalId, String uri) {
        this(internalId, externalId, uri, null, null);
    }
}
