package org.openmrs.module.fhir.mapper.bundler;


import ca.uhn.fhir.model.dstu2.resource.Encounter;
import org.openmrs.Obs;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.util.List;

public interface EmrObsResourceHandler {
    boolean canHandle(Obs observation);
    List<FHIRResource> map(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties);

}
