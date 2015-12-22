package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.openmrs.Order;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.util.List;

public interface EmrOrderResourceHandler {
    boolean canHandle(Order order);

    List<FHIRResource> map(Order order, FHIREncounter fhirEncounter, Bundle bundle, SystemProperties systemProperties);
}
