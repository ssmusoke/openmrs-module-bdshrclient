package org.openmrs.module.fhir.utils;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class CodableConceptService {

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    public CodeableConceptDt getFHIRCodeableConcept(String code, String system, String display) {
        CodeableConceptDt codeableConcept = new CodeableConceptDt();
        addFHIRCoding(codeableConcept, code, system, display);
        return codeableConcept;
    }

    public void addFHIRCoding(CodeableConceptDt codeableConcept, String code, String system, String display) {
        CodingDt coding = codeableConcept.addCoding();
        coding.setCode(code);
        coding.setSystem(system);
        coding.setDisplay(display);
    }

    public CodeableConceptDt addTRCodingOrDisplay(Concept concept, IdMappingsRepository idMappingsRepository) {
        CodeableConceptDt codeableConceptDt = addTRCoding(concept, idMappingsRepository);
        if (CollectionUtils.isEmpty(codeableConceptDt.getCoding())) {
            CodingDt coding = codeableConceptDt.addCoding();
            coding.setDisplay(concept.getName().getName());
        }
        return codeableConceptDt;
    }

    public CodeableConceptDt addTRCoding(Concept concept, IdMappingsRepository idMappingsRepository) {
        CodeableConceptDt codeableConcept = new CodeableConceptDt();
        Collection<ConceptMap> conceptMappings = concept.getConceptMappings();
        for (ConceptMap mapping : conceptMappings) {
            addTRCodingsForReferenceTerms(concept, idMappingsRepository, codeableConcept, mapping);
        }
        addTRCodingForConcept(concept, idMappingsRepository, codeableConcept);
        return codeableConcept;
    }

    private void addTRCodingForConcept(Concept concept, IdMappingsRepository idMappingsRepository, CodeableConceptDt codeableConcept) {
        IdMapping idMapping = idMappingsRepository.findByInternalId(concept.getUuid());
        if (idMapping != null) {
            addFHIRCoding(codeableConcept, idMapping.getExternalId(), idMapping.getUri(), concept.getName().getName());
        }
    }

    private void addTRCodingsForReferenceTerms(Concept concept, IdMappingsRepository idMappingsRepository, CodeableConceptDt codeableConcept, ConceptMap mapping) {
        ConceptReferenceTerm conceptReferenceTerm = mapping.getConceptReferenceTerm();
        IdMapping idMapping = idMappingsRepository.findByInternalId(conceptReferenceTerm.getUuid());
        if (idMapping != null) {
            addFHIRCoding(codeableConcept, conceptReferenceTerm.getCode(), idMapping.getUri(), concept.getName().getName());
        }
    }

    public String getTRValueSetCode(Concept concept) {
        for (ConceptMap mapping : concept.getConceptMappings()) {
            if (mapping.getConceptMapType().getUuid().equals(ConceptMapType.SAME_AS_MAP_TYPE_UUID)) {
                return mapping.getConceptReferenceTerm().getCode();
            }
        }
        for (ConceptName conceptName : concept.getShortNames()) {
            return conceptName.getName();
        }
        return concept.getName().getName();
    }

    public CodeableConceptDt getTRValueSetCodeableConcept(Concept concept, String valueSetURL) {
        return getTRValueSetCodeableConcept(concept, valueSetURL, new CodeableConceptDt());
    }

    public CodeableConceptDt getTRValueSetCodeableConcept(Concept concept, String valueSetURL, CodeableConceptDt codeableConcept) {
        CodingDt coding = codeableConcept.addCoding();
        if (null != idMappingsRepository.findByInternalId(concept.getUuid())) {
            coding.setCode(getTRValueSetCode(concept));
            coding.setSystem(valueSetURL);
        }
        coding.setDisplay(concept.getName().getName());
        return codeableConcept;
    }

}
