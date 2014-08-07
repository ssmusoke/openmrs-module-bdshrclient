package org.openmrs.module.fhir.mapper.model;

public interface PartOf<Aggregate> {

    public Aggregate mergeWith(Aggregate aggregate);
}
