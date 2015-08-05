package org.openmrs.module.fhir.mapper.model;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Obs;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;

public class CompoundObservation {
    private Obs observation;
    private GlobalPropertyLookUpService globalPropertyLookUpService;

    public CompoundObservation(Obs observation, GlobalPropertyLookUpService globalPropertyLookUpService) {
        this.observation = observation;
        this.globalPropertyLookUpService = globalPropertyLookUpService;
    }

    public boolean isOfType(ObservationType type) {
        if (StringUtils.isNotEmpty(type.getConceptIdKey())) {
            Integer conceptId = globalPropertyLookUpService.getGlobalPropertyValue(type.getConceptIdKey());
            return observation.getConcept().getId().equals(conceptId);
        }
        return observation.getConcept().getName().getName().equalsIgnoreCase(type.getConceptName());
    }

    public Obs findMember(ObservationType type) {
        for (Obs member : observation.getGroupMembers()) {
            if (type.getConceptIdKey() != null) {
                Integer conceptId = globalPropertyLookUpService.getGlobalPropertyValue(type.getConceptIdKey());
                if (member.getConcept().getId().equals(conceptId)) {
                    return member;
                }
            }
            if (member.getConcept().getName().getName().equalsIgnoreCase(type.getConceptName())) {
                return member;
            }
        }
        return null;
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
