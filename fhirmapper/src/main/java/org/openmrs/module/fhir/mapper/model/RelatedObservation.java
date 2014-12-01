package org.openmrs.module.fhir.mapper.model;

import org.hl7.fhir.instance.model.Observation;
import org.hl7.fhir.instance.model.ResourceReference;

public class RelatedObservation implements PartOf<Observation> {

    private Observation relatedObservation;

    public RelatedObservation(Observation relatedObservation) {
        this.relatedObservation = relatedObservation;
    }

    @Override
    public Observation mergeWith(Observation observation) {
        Observation.ObservationRelatedComponent related = observation.addRelated();
        related.setTarget(new ResourceReference().setReference(relatedObservation.getIdentifier().getValue()));
        related.setTypeSimple(Observation.ObservationRelationshiptypes.hascomponent);
        return observation;
    }

}
