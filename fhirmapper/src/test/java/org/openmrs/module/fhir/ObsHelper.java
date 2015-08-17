package org.openmrs.module.fhir;

import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;

public class ObsHelper {
    public Obs findMemberObsByConceptName(Obs observation, String conceptName, GlobalPropertyLookUpService globalPropertyLookUpService) {
        CompoundObservation compoundObservation = new CompoundObservation(observation);
        return compoundObservation.getMemberObsForConceptName(conceptName);
    }
}
