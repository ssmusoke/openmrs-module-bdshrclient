package org.openmrs.module.fhir.utils;

import org.openmrs.Concept;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.MRSProperties;

import java.util.List;

public class OMRSHelpers {

    public static Concept identifyConceptFromReferenceCodes(String code, String system, String conceptName, ConceptService conceptService) {
        //TODO : Use proper Concept Source.
        if (system.startsWith("ICD10")) {
            ConceptSource conceptSource = conceptService.getConceptSourceByName(MRSProperties.MRS_CONCEPT_SOURCE_NAME_FOR_ICD10);
            List<Concept> conceptsByMapping = conceptService.getConceptsByMapping(code, conceptSource.getName());
            if ((conceptsByMapping != null) && !conceptsByMapping.isEmpty()) {
                Concept mappedConcept = null;
                for (Concept concept : conceptsByMapping) {
                    if (concept.getName().getName().equalsIgnoreCase(conceptName)) {
                        return concept;
                    }
                }
                if (mappedConcept == null) {
                    return conceptsByMapping.get(0);
                }
            }
        }
        return conceptService.getConceptByName(conceptName);
    }
}
