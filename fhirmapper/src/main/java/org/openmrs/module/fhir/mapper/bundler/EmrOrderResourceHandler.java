package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Composition;
import org.hl7.fhir.instance.model.Encounter;
import org.openmrs.Order;

import java.util.List;

public interface EmrOrderResourceHandler {
    boolean handles(Order order);
    EmrResource map(Order order, Encounter fhirEncounter, AtomFeed feed);
}
