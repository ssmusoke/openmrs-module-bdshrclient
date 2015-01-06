package org.openmrs.module.fhir.utils;

import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.openmrs.*;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class CodableConceptService {

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    public CodeableConcept getFHIRCodeableConcept(String code, String system, String display) {
        CodeableConcept codeableConcept = new CodeableConcept();
        addFHIRCoding(codeableConcept, code, system, display);
        return codeableConcept;
    }

    public void addFHIRCoding(CodeableConcept codeableConcept, String code, String system, String display) {
        Coding coding = codeableConcept.addCoding();
        coding.setCodeSimple(code);
        coding.setSystemSimple(system);
        coding.setDisplaySimple(display);
    }

    public CodeableConcept addTRCoding(Concept concept, IdMappingsRepository idMappingsRepository) {
        CodeableConcept codeableConcept = new CodeableConcept();
        Collection<ConceptMap> conceptMappings = concept.getConceptMappings();
        for (ConceptMap mapping : conceptMappings) {
            addTRCodingsForReferenceTerms(concept, idMappingsRepository, codeableConcept, mapping);
        }
        addTRCodingForConcept(concept, idMappingsRepository, codeableConcept);
        return codeableConcept;
    }

    private void addTRCodingForConcept(Concept concept, IdMappingsRepository idMappingsRepository, CodeableConcept codeableConcept) {
        IdMapping idMapping = idMappingsRepository.findByInternalId(concept.getUuid());
        if (idMapping != null) {
            addFHIRCoding(codeableConcept, idMapping.getExternalId(), idMapping.getUri(), concept.getName().getName());
        }
    }

    private void addTRCodingsForReferenceTerms(Concept concept, IdMappingsRepository idMappingsRepository, CodeableConcept codeableConcept, ConceptMap mapping) {
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

    public CodeableConcept getTRValueSetCodeableConcept(Concept concept, String valueSetURL) {
        return getTRValueSetCodeableConcept(concept, valueSetURL, new CodeableConcept());
    }

    public CodeableConcept getTRValueSetCodeableConcept(Concept concept, String valueSetURL, CodeableConcept codeableConcept) {
        Coding coding = codeableConcept.addCoding();
        if (null != idMappingsRepository.findByInternalId(concept.getUuid())) {
            coding.setCodeSimple(getTRValueSetCode(concept));
            coding.setSystemSimple(valueSetURL);
        }
        coding.setDisplaySimple(concept.getName().getName());
        return codeableConcept;
    }

}
