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

    public Obs getMemberObsForConceptName(String conceptName) {
        for (Obs groupMember : this.observation.getGroupMembers()) {
            if (conceptName.equals(groupMember.getConcept().getName().getName())) {
                return groupMember;
            }
        }
        return null;
    }

    public String getUuid() {
        return observation.getUuid();
    }

    public Obs getRawObservation() {
        return observation;
    }
}
