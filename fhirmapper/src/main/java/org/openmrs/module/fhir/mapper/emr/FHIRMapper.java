package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
public class FHIRMapper {
    @Autowired
    FHIREncounterMapper fhirEncounterMapper;

    public Encounter map(Patient emrPatient, String healthId, String fhirEncounterId, Bundle bundle, SystemProperties systemProperties) throws ParseException {
        return fhirEncounterMapper.map(healthId, fhirEncounterId, emrPatient, bundle, systemProperties);
    }
}
