package org.openmrs.module.fhir.mapper.bundler;


import org.hl7.fhir.instance.model.Encounter;
import org.openmrs.Obs;

import java.util.List;

public interface EmrResourceHandler {
    boolean handles(Obs observation);
    List<EmrResource> map(Obs obs, Encounter fhirEncounter);
}
