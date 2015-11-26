package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.fhir.mapper.model.ShrEncounterComposition;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.stereotype.Component;

@Component
public interface FHIRResourceMapper {
    public boolean canHandle(IResource resource);

    public void map(IResource resource, Encounter newEmrEncounter, ShrEncounterComposition encounterComposition, SystemProperties systemProperties);
}
