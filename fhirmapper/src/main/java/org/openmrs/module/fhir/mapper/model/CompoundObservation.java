package org.openmrs.module.fhir.mapper.model;

import org.openmrs.Obs;

public class CompoundObservation {

    private Obs observation;

    public CompoundObservation(Obs observation) {
        this.observation = observation;
    }

    public boolean isOfType(ObservationType type) {
        return observation.getConcept().getName().getName().equalsIgnoreCase(type.getDisplayName());
    }

    public Obs findMember(ObservationType type) {
        for (Obs member : observation.getGroupMembers()) {
            if (member.getConcept().getName().getName().equalsIgnoreCase(type.getDisplayName())) {
                return member;
            }
        }
        return null;
    }

}
