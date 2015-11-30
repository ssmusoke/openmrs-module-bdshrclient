package org.openmrs.module.fhir.mapper.emr;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.fhir.mapper.model.ShrEncounter;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
public class FHIRMapper {
    @Autowired
    FHIREncounterMapper fhirEncounterMapper;

    public Encounter map(Patient emrPatient, ShrEncounter encounterComposition, SystemProperties systemProperties) throws ParseException {
        return fhirEncounterMapper.map(emrPatient, encounterComposition, systemProperties);
    }
}
