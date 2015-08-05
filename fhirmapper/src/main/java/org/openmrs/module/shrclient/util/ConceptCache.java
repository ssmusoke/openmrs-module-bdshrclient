package org.openmrs.module.shrclient.util;

import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;

import java.util.HashMap;
import java.util.Map;

public class ConceptCache {
    private ConceptService conceptService;
    private Map<String, Concept> conceptMap;
    private GlobalPropertyLookUpService globalPropertyLookUpService;

    public ConceptCache(ConceptService conceptService, GlobalPropertyLookUpService globalPropertyLookUpService) {
        this.conceptMap = new HashMap<>();
        this.conceptService = conceptService;
        this.globalPropertyLookUpService = globalPropertyLookUpService;
    }

    public Concept getConceptFromGlobalProperty(String propertyName) {
        if (this.conceptMap.containsKey(propertyName)) {
            return this.conceptMap.get(propertyName);
        }
        Concept concept = getConceptFromConfiguredGlobalProperty(propertyName);
        if (concept != null) {
            this.conceptMap.put(propertyName, concept);
        }
        return concept;
    }

    private Concept getConceptFromConfiguredGlobalProperty(String propertyName) {
        Integer value = globalPropertyLookUpService.getGlobalPropertyValue(propertyName);
        if(value != null){
            return conceptService.getConcept(value);
        }
        return null;
    }

}
