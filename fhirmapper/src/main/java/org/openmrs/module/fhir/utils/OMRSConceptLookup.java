package org.openmrs.module.fhir.utils;

import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Drug;
import org.openmrs.api.ConceptService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.collections4.CollectionUtils.exists;
import static org.openmrs.module.fhir.utils.Constants.ID_MAPPING_CONCEPT_TYPE;
import static org.openmrs.module.fhir.utils.Constants.ID_MAPPING_REFERENCE_TERM_TYPE;

@Component
public class OMRSConceptLookup {

    private ConceptService conceptService;
    private IdMappingsRepository idMappingsRepository;
    private GlobalPropertyLookUpService globalPropertyLookUpService;

    @Autowired
    public OMRSConceptLookup(ConceptService conceptService, IdMappingsRepository repository, GlobalPropertyLookUpService globalPropertyLookUpService) {
        this.conceptService = conceptService;
        this.idMappingsRepository = repository;
        this.globalPropertyLookUpService = globalPropertyLookUpService;
    }

    public Concept findConcept(List<CodingDt> codings) {
        Map<ConceptReferenceTerm, String> referenceTermMap = new HashMap<>();
        for (CodingDt coding : codings) {
            if (isValueSetUrl(coding.getSystem())) {
                Concept concept = findConceptFromValueSetCode(coding.getSystem(), coding.getCode());
                if (concept != null) return concept;
            } else {
                String uuid = getUuid(coding.getSystem());
                if (StringUtils.isNotBlank(uuid)) {
                    IdMapping idMapping = idMappingsRepository.findByExternalId(uuid);
                    if (idMapping != null) {
                        if (ID_MAPPING_CONCEPT_TYPE.equalsIgnoreCase(idMapping.getType())) {
                            return conceptService.getConceptByUuid(idMapping.getInternalId());
                        } else if (ID_MAPPING_REFERENCE_TERM_TYPE.equalsIgnoreCase(idMapping.getType())) {
                            referenceTermMap.put(conceptService.getConceptReferenceTermByUuid(idMapping.getInternalId()), coding.getDisplay());
                        }
                    }
                }
            }
        }
        return findConceptByReferenceTermMapping(referenceTermMap);
    }

    public Drug findDrug(List<CodingDt> codings) {
        Drug drug = null;
        for (CodingDt coding : codings) {
            if (isDrugSet(coding.getSystem())) {
                drug = findDrug(coding.getCode());
                if (drug != null) return drug;
            }
        }
        return drug;
    }

    private boolean isValueSetUrl(String systemSimple) {
        return StringUtils.contains(systemSimple, "tr/vs/");
    }

    private boolean isDrugSet(String systemSimple) {
        return StringUtils.contains(systemSimple, "tr/drugs/");
    }

    public Drug findDrug(String drugExternalId) {
        IdMapping idMapping = idMappingsRepository.findByExternalId(drugExternalId);
        if (idMapping != null) {
            return conceptService.getDrugByUuid(idMapping.getInternalId());
        }
        return null;
    }

    public Concept findConceptFromValueSetCode(String system, String code) {
        String valueSet = StringUtils.replace(StringUtils.substringAfterLast(system, "/"), "-", " ");
        Concept valueSetConcept = conceptService.getConceptByName(valueSet);
        if (valueSetConcept != null) {
            for (ConceptAnswer answer : valueSetConcept.getAnswers()) {
                Concept concept = answer.getAnswerConcept();
                if (referenceTermCodeFound(concept, code) || shortNameFound(concept, code) || fullNameMatchFound(concept, code))
                    return concept;
            }
        }
        return conceptService.getConceptByName(code);
    }

    private boolean fullNameMatchFound(Concept concept, String code) {
        return concept.getName().getName().equals(code);
    }

    public boolean referenceTermCodeFound(Concept concept, final String code) {
        return exists(concept.getConceptMappings(), new Predicate<ConceptMap>() {
            @Override
            public boolean evaluate(ConceptMap conceptMap) {
                return conceptMap.getConceptReferenceTerm().getCode().equals(code);
            }
        });
    }

    public boolean shortNameFound(Concept concept, final String code) {
        return exists(concept.getShortNames(), new Predicate<ConceptName>() {
            @Override
            public boolean evaluate(ConceptName conceptName) {
                return conceptName.getName().equals(code);
            }
        });
    }

    public Concept findTRConceptOfType(TrValueSetType type) {
        String globalPropertyValue = globalPropertyLookUpService.getGlobalPropertyValue(type.getGlobalPropertyKey());
        if (globalPropertyValue != null) {
            return conceptService.getConcept(Integer.parseInt(globalPropertyValue));
        } else {
            return conceptService.getConceptByName(type.getDefaultConceptName());
        }
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
        return null;
    }

    private static String getUuid(String content) {
        return StringUtils.substringAfterLast(content, "/");
    }
}
