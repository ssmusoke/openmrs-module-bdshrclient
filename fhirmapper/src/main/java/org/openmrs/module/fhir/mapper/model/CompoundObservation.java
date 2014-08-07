package org.openmrs.module.fhir.mapper.model;

import org.openmrs.Obs;

import java.util.ArrayList;
import java.util.List;

public class CompoundObservation {

    private Obs observation;

    public CompoundObservation(Obs observation) {
        this.observation = observation;
    }

    public boolean isOfType(ObservationType type) {
        return observation.getConcept().getName().getName().equalsIgnoreCase(type.getDisplayName());
    }

    public Obs findMember(ObservationType type) {
        Obs result = null;
        for (Obs member : observation.getGroupMembers()) {
            if (member.getConcept().getName().getName().equalsIgnoreCase(type.getDisplayName())) {
                result = member;
            }
        }
        return result;
    }

    public List<Obs> findMembers(ObservationType type) {
        List<Obs> result = new ArrayList<Obs>();
        for (Obs member : observation.getGroupMembers()) {
            if (member.getConcept().getName().getName().equalsIgnoreCase(type.getDisplayName())) {
                result.add(member);
            }
        }
        return result;
    }
}
