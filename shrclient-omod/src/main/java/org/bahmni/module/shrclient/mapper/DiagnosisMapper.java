package org.bahmni.module.shrclient.mapper;

import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Date;
import org.hl7.fhir.instance.model.Enumeration;
import org.openmrs.Concept;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Obs;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DiagnosisMapper {

    private final Map<String,Condition.ConditionStatus> diaConditionStatus = new HashMap<String, Condition.ConditionStatus>();
    private final Map<String,String> diaConditionSeverity = new HashMap<String, String>();
    private final FHIRProperties fhirProperties;

    public DiagnosisMapper() {
        fhirProperties = new FHIRProperties();
        diaConditionStatus.put(MRSProperties.MRS_DIAGNOSIS_STATUS_PRESUMED, Condition.ConditionStatus.provisional);
        diaConditionStatus.put(MRSProperties.MRS_DIAGNOSIS_STATUS_CONFIRMED, Condition.ConditionStatus.confirmed);

        diaConditionSeverity.put(MRSProperties.MRS_DIAGNOSIS_SEVERITY_PRIMARY, FHIRProperties.FHIR_SEVERITY_MODERATE);
        diaConditionSeverity.put(MRSProperties.MRS_DIAGNOSIS_SEVERITY_SECONDARY, FHIRProperties.FHIR_SEVERITY_SEVERE);

    }

    public List<Condition> map(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        Set<Obs> allObs = openMrsEncounter.getAllObs(true);
        List<Condition> diagnoses = new ArrayList<Condition>();
        for (Obs obs : allObs) {
            if (isDiagnosisObservation(obs)) {
                diagnoses.add(createFHIRCondition(encounter, obs));
            }
        }
        return diagnoses;

    }

    private Condition createFHIRCondition(Encounter encounter, Obs obs) {
        Condition condition = new Condition();
        condition.setEncounter(encounter.getIndication());
        condition.setSubject(encounter.getSubject());
        condition.setAsserter(getParticipant(encounter));
        condition.setCategory(getDiagnosisCategory());

        final Set<Obs> obsMembers = obs.getGroupMembers(false);
        for (Obs member : obsMembers) {
            Concept memberConcept = member.getConcept();
            if (isCodedDiagnosisObservation(memberConcept)) {
                condition.setCode(getDiagnosisCode(member.getValueCoded()));
            }
            else if (isDiagnosisCertaintyObservation(memberConcept)) {
                condition.setStatus(getConditionStatus(member));
            }
            else if (isDiagnosisOrderObservation(memberConcept)) {
                condition.setSeverity(getDiagnosisSeverity(member.getValueCoded()));
            }
        }

        Date onsetDate = new Date();
        onsetDate.setValue(new DateAndTime(obs.getObsDatetime()));
        condition.setDateAsserted(onsetDate);

        Identifier identifier = condition.addIdentifier();
        identifier.setValueSimple(obs.getUuid());
        return condition;
    }

    private boolean isDiagnosisOrderObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_ORDER);
    }

    private boolean isDiagnosisCertaintyObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_CERTAINTY);
    }

    private boolean isCodedDiagnosisObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_CODED_DIAGNOSIS);
    }

    private boolean isDiagnosisObservation(Obs obs) {
        return obs.getConcept().getName().getName().equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_VISIT_DIAGNOSES);
    }


    private ResourceReference getParticipant(Encounter encounter) {
        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
        if ((participants != null) && !participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }

    private Enumeration<Condition.ConditionStatus> getConditionStatus(Obs member) {
        Concept diagnosisStatus = member.getValueCoded();
        Condition.ConditionStatus status = diaConditionStatus.get(diagnosisStatus.getName().getName());
        if (status != null) {
            return new Enumeration<Condition.ConditionStatus>(status);
        } else {
            return new Enumeration<Condition.ConditionStatus>(Condition.ConditionStatus.confirmed);
        }
    }

    private CodeableConcept getDiagnosisCode(Concept obsConcept) {
        CodeableConcept diagnosisCode = new CodeableConcept();
        Coding coding = diagnosisCode.addCoding();
        //TODO to change to reference term code
        ConceptCoding refCoding = getReferenceCode(obsConcept);
        coding.setCodeSimple(refCoding.code);
        coding.setSystemSimple(refCoding.source);
        coding.setDisplaySimple(obsConcept.getName().getName());
        return diagnosisCode;
    }

    private class ConceptCoding {
        String code;
        String source;
    }

    private ConceptCoding getReferenceCode(Concept obsConcept) {
        Collection<org.openmrs.ConceptMap> conceptMappings = obsConcept.getConceptMappings();
        for (org.openmrs.ConceptMap mapping : conceptMappings) {
            //TODO right now returning the first matching term
            ConceptCoding coding = new ConceptCoding();
            ConceptReferenceTerm conceptReferenceTerm = mapping.getConceptReferenceTerm();
            coding.code = conceptReferenceTerm.getCode();
            coding.source = conceptReferenceTerm.getConceptSource().getName();
            return coding;
        }
        ConceptCoding defaultCoding = new ConceptCoding();
        defaultCoding.code = obsConcept.getUuid();
        //TODO: put in the right URL. To be mapped
        //TODO temporary. To read TR concept URL from mapping
        defaultCoding.source = org.bahmni.module.shrclient.util.Constants.TERMINOLOGY_SERVER_CONCEPT_URL + obsConcept.getUuid();
        return defaultCoding;
    }

    private CodeableConcept getDiagnosisSeverity(Concept valueCoded) {
        CodeableConcept conditionSeverity = new CodeableConcept();
        Coding coding = conditionSeverity.addCoding();
        String severity = diaConditionSeverity.get(valueCoded.getName().getName());
        if (severity != null) {
            coding.setDisplaySimple(severity);
            coding.setCodeSimple(fhirProperties.getSeverityCode(severity));
        } else {
            coding.setDisplaySimple(FHIRProperties.FHIR_SEVERITY_MODERATE);
            coding.setCodeSimple(fhirProperties.getSeverityCode(FHIRProperties.FHIR_SEVERITY_MODERATE));
        }
        coding.setSystemSimple(FHIRProperties.FHIR_CONDITION_SEVERITY_URL);
        return conditionSeverity;
    }

    private CodeableConcept getDiagnosisCategory() {
        CodeableConcept conditionCategory = new CodeableConcept();
        Coding coding = conditionCategory.addCoding();
        coding.setCodeSimple(FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS);
        coding.setSystemSimple(FHIRProperties.FHIR_CONDITION_CATEGORY_URL);
        coding.setDisplaySimple(FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS);
        return conditionCategory;
    }
}
