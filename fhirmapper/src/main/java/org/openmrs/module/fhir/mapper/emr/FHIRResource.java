package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Resource;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public interface FHIRResource {
    public boolean canHandle(Resource resource);

    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter, HashMap<String, String> processedList);
}
