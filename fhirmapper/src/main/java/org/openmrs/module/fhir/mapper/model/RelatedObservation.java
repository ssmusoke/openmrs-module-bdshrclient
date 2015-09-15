package org.openmrs.module.fhir.mapper.model;

import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.valueset.ObservationRelationshipTypeEnum;
import org.openmrs.module.shrclient.util.SystemProperties;

public class RelatedObservation implements PartOf<Observation> {

    private Observation relatedObservation;

    public RelatedObservation(Observation relatedObservation) {
        this.relatedObservation = relatedObservation;
    }

    @Override
    public Observation mergeWith(Observation observation, SystemProperties systemProperties) {
        Observation.Related related = observation.addRelated();
        related.setTarget(new ResourceReferenceDt().setReference(relatedObservation.getId().getValue()));
        related.setType(ObservationRelationshipTypeEnum.HAS_MEMBER);
        return observation;
    }

}
