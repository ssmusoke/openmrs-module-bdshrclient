package org.openmrs.module.shrclient.model;

public class DiagnosisIdMapping extends IdMapping {

    public DiagnosisIdMapping(String internalId, String externalId, String uri) {
        super(internalId, externalId, IdMappingType.DIAGNOSIS, uri);
    }

}
