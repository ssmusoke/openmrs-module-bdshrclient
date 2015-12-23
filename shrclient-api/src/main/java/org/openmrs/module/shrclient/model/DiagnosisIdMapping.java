package org.openmrs.module.shrclient.model;

import java.util.Date;

public class DiagnosisIdMapping extends IdMapping {

    public DiagnosisIdMapping(String internalId, String externalId, String uri) {
        super(internalId, externalId, IdMappingType.DIAGNOSIS, uri);
    }

}
