package org.openmrs.module.fhir.utils;

import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.instance.model.Coding;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.api.ConceptService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Locale.ENGLISH;
import static org.openmrs.module.fhir.utils.Constants.ID_MAPPING_CONCEPT_TYPE;
import static org.openmrs.module.fhir.utils.Constants.ID_MAPPING_REFERENCE_TERM_TYPE;

@Component
public class OMRSHelper {

    private ConceptService conceptService;
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    public OMRSHelper(ConceptService conceptService, IdMappingsRepository repository) {
        this.conceptService = conceptService;
        this.idMappingsRepository = repository;
    }

    public Concept findConcept(List<Coding> codings) {
        Map<ConceptReferenceTerm, String> referenceTermMap = new HashMap<ConceptReferenceTerm, String>();
        for (Coding coding : codings) {
            String uuid = getUuid(coding.getSystemSimple());
            if (StringUtils.isNotBlank(uuid)) {
                IdMapping idMapping = idMappingsRepository.findByExternalId(uuid);
                if (idMapping != null) {
                    if (ID_MAPPING_CONCEPT_TYPE.equalsIgnoreCase(idMapping.getType())) {
                        return conceptService.getConceptByUuid(idMapping.getInternalId());
                    } else if (ID_MAPPING_REFERENCE_TERM_TYPE.equalsIgnoreCase(idMapping.getType())) {
                        referenceTermMap.put(conceptService.getConceptReferenceTermByUuid(idMapping.getInternalId()), coding.getDisplaySimple());
                    }
                }
            }
        }
        return findConceptByReferenceTermMapping(referenceTermMap);
    }

    private Concept findConceptByReferenceTermMapping(Map<ConceptReferenceTerm, String> referenceTermMapping) {
        if (referenceTermMapping.size() == 0) {
            return null;
        }

        ConceptReferenceTerm refTerm = null;
        for (ConceptReferenceTerm referenceTerm : referenceTermMapping.keySet()) {
            refTerm = referenceTerm;
            List<Concept> concepts = conceptService.getConceptsByMapping(referenceTerm.getCode(), referenceTerm.getConceptSource().getName());
            for (Concept concept : concepts) {
                if (concept.getName().getName().equalsIgnoreCase(referenceTermMapping.get(referenceTerm))) {
                    return concept;
                }
            }
        }

        // TODO: This mapping is incomplete
        final String conceptName = referenceTermMapping.get(refTerm);
        Concept concept = new Concept();
        concept.addName(new ConceptName(conceptName, ENGLISH));
        concept.addConceptMapping(new ConceptMap(refTerm, conceptService.getConceptMapTypeByName("SAME-AS")));
        return conceptService.saveConcept(concept);
    }

    private static String getUuid(String content) {
        return StringUtils.substringAfterLast(content, "/");
    }
}
