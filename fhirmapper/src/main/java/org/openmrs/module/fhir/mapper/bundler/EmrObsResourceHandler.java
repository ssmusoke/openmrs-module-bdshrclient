package org.openmrs.module.fhir.mapper.bundler;


import org.hl7.fhir.instance.model.Encounter;
import org.openmrs.Obs;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.util.List;

public interface EmrObsResourceHandler {
    boolean canHandle(Obs observation);
    List<EmrResource> map(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties);

}
