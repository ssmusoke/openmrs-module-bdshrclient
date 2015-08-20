package org.openmrs.module.fhir.mapper.model;

import ca.uhn.fhir.model.dstu2.resource.Observation;
import org.openmrs.module.shrclient.util.SystemProperties;

public interface PartOf<Aggregate> {

    public Aggregate mergeWith(Observation observation, SystemProperties systemProperties);
}
