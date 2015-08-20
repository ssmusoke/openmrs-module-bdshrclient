package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public interface FHIRResourceMapper {
    public boolean canHandle(IResource resource);

    public void map(Bundle bundle, IResource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList);
}
