package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Encounter;
import org.openmrs.Order;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.util.List;

public interface EmrOrderResourceHandler {
    boolean canHandle(Order order);
    List<FHIRResource> map(Order order, Encounter fhirEncounter, AtomFeed feed, SystemProperties systemProperties);
}
