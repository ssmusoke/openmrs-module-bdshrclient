package org.bahmni.module.shrclient.mapper;

import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Enumeration;
import org.openmrs.Concept;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Obs;

import java.util.*;

public class DiagnosisMapper {

    public static final String FHIR_CONDITION_CODE_DIAGNOSIS = "diagnosis";
    public static final String TERMINOLOGY_SERVER_CONCEPT_URL = "http://192.168.33.18/openmrs/ws/rest/v1/concept/";
    public static final String FHIR_CONDITION_CATEGORY_URL = "http://hl7.org/fhir/vs/condition-category";
    public static final String FHIR_CONDITION_SEVERITY_URL = "http://hl7.org/fhir/vs/condition-severity";
    public static final String SEVERITY_MODERATE = "Moderate";
    public static final String SEVERITY_SEVERE = "Severe";
    public static final String SNOMED_VALUE_MODERATE_SEVERTY = "6736007";
    public static final String SNOMED_VALUE_SEVERE_SEVERITY = "24484000";
    public static final String DIAGNOSIS_PRESUMED = "Presumed";
    public static final String DIAGNOSIS_CONFIRMED = "Confirmed";
    public static final String MRS_SEVERITY_PRIMARY = "Primary";
    public static final String MRS_SEVERITY_SECONDARY = "Secondary";

    private Map<String, String> severityCodes = new HashMap<String, String>();
    private final Map<String,Condition.ConditionStatus> diaConditionStatus = new HashMap<String, Condition.ConditionStatus>();
    private final Map<String,String> diaConditionSeverity = new HashMap<String, String>();


//+--------+--------------+----------+--------------------------+------------+-------------+--------------------+
//| obs_id | obs_group_id | class    | name                     | concept_id | value_coded | name               |
//+--------+--------------+----------+--------------------------+------------+-------------+--------------------+
//|     84 |         NULL | ConvSet  | Visit Diagnoses          |         13 |        NULL | NULL               |
//|     85 |           84 | Question | Diagnosis order          |         19 |          21 | Primary/Secondary  |
//|     86 |           84 | Question | Diagnosis Certainty      |         16 |          18 | Confirmed/Presumed |
//|     87 |           84 | Question | Coded Diagnosis          |         15 |         181 | TestWithoutRefTerm |
//|     88 |           84 | Misc     | Bahmni Initial Diagnosis |         50 |        NULL | NULL               |
//|     89 |           84 | Misc     | Bahmni Diagnosis Status  |         49 |        NULL | NULL               |
//|     90 |           84 | Misc     | Bahmni Diagnosis Revised |         51 |           2 | False              |
//|     90 |           84 | Misc     | Bahmni Diagnosis Revised |         51 |           2 | No                 |
//+--------+--------------+----------+--------------------------+------------+-------------+--------------------+


    public DiagnosisMapper() {
        severityCodes.put(SEVERITY_MODERATE, SNOMED_VALUE_MODERATE_SEVERTY);
        severityCodes.put(SEVERITY_SEVERE, SNOMED_VALUE_SEVERE_SEVERITY);

        diaConditionStatus.put(DIAGNOSIS_PRESUMED, Condition.ConditionStatus.provisional);
        diaConditionStatus.put(DIAGNOSIS_CONFIRMED, Condition.ConditionStatus.confirmed);

        diaConditionSeverity.put(MRS_SEVERITY_PRIMARY, SEVERITY_MODERATE);
        diaConditionSeverity.put(MRS_SEVERITY_SECONDARY, SEVERITY_SEVERE);

    }

    public List<Condition> map(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        Set<Obs> allObs = openMrsEncounter.getAllObs(true);
        List<Condition> diagnoses = new ArrayList<Condition>();
        for (Obs obs : allObs) {
            if (obs.getConcept().getName().getName().equalsIgnoreCase("Visit Diagnoses")) {
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
            final String memberConceptName = member.getConcept().getName().getName();
            if (memberConceptName.equalsIgnoreCase("Coded Diagnosis")) {
                condition.setCode(getDiagnosisCode(member.getValueCoded()));
            }
            else if (memberConceptName.equalsIgnoreCase("Diagnosis Certainty")) {
                condition.setStatus(getConditionStatus(member));
            }
            else if (memberConceptName.equalsIgnoreCase("Diagnosis order")) {
                condition.setSeverity(getDiagnosisSeverity(member.getValueCoded()));
            }
        }

        DateTime onsetDate = new DateTime();
        onsetDate.setValue(new DateAndTime(obs.getObsDatetime()));
        condition.setOnset(onsetDate);

        Identifier identifier = condition.addIdentifier();
        identifier.setValueSimple(obs.getUuid());
        return condition;
    }

    private ResourceReference getParticipant(Encounter encounter) {
        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
        if ((participants != null) || participants.isEmpty()) {
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
        defaultCoding.source = TERMINOLOGY_SERVER_CONCEPT_URL + obsConcept.getUuid();
        return defaultCoding;
    }

    private CodeableConcept getDiagnosisSeverity(Concept valueCoded) {
        CodeableConcept conditionSeverity = new CodeableConcept();
        Coding coding = conditionSeverity.addCoding();
        String severity = diaConditionSeverity.get(valueCoded.getName().getName());
        if (severity != null) {
            coding.setDisplaySimple(severity);
            coding.setCodeSimple(severityCodes.get(severity));
        } else {
            coding.setDisplaySimple(SEVERITY_MODERATE);
            coding.setCodeSimple(severityCodes.get(SEVERITY_MODERATE));
        }
        coding.setSystemSimple(FHIR_CONDITION_SEVERITY_URL);
        return conditionSeverity;
    }

    private CodeableConcept getDiagnosisCategory() {
        CodeableConcept conditionCategory = new CodeableConcept();
        Coding coding = conditionCategory.addCoding();
        coding.setCodeSimple(FHIR_CONDITION_CODE_DIAGNOSIS);
        coding.setSystemSimple(FHIR_CONDITION_CATEGORY_URL);
        coding.setDisplaySimple("diagnosis");
        return conditionCategory;
    }
}
