package org.bahmni.module.shrclient.mapper;

import org.bahmni.module.shrclient.util.Constants;
import org.hl7.fhir.instance.model.*;
import org.joda.time.DateTime;
import org.openmrs.Concept;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Obs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ChiefComplaintMapper {

    public static final String TERMINOLOGY_SERVER_CONCEPT_URL = "http://192.168.33.18/openmrs/ws/rest/v1/concept/";

    public List<Condition> map(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        Set<Obs> allObs = openMrsEncounter.getObsAtTopLevel(false);
        List<Condition> chiefComplaints = new ArrayList<Condition>();
        for (Obs obs : allObs) {
            if (obs.getConcept().getName().getName().equalsIgnoreCase("History and Examination")) {
                for (Obs member : obs.getGroupMembers()) {
                    if (obs.getConcept().getName().getName().equalsIgnoreCase("Chief Complaint Data")) {
                        chiefComplaints.add(createFHIRCondition(encounter, member));
                    }
                }
            }
        }
        return chiefComplaints;
    }

    private Condition createFHIRCondition(Encounter encounter, Obs obs) {
        Condition condition = new Condition();
        condition.setEncounter(encounter.getIndication());
        condition.setSubject(encounter.getSubject());
        condition.setAsserter(getParticipant(encounter));
        condition.setCategory(getChiefComplaintCategory());
        condition.setSeverity(getChiefComplaintSevirity());
        condition.setStatus(new Enumeration<Condition.ConditionStatus>(Condition.ConditionStatus.confirmed));

        org.hl7.fhir.instance.model.Date assertedDate = new org.hl7.fhir.instance.model.Date();
        assertedDate.setValue(new DateAndTime(obs.getObsDatetime()));
        condition.setDateAsserted(assertedDate);

        final Set<Obs> obsMembers = obs.getGroupMembers(false);
        for (Obs member : obsMembers) {
            final String memberConceptName = member.getConcept().getName().getName();
            if (memberConceptName.equalsIgnoreCase("Chief Complaint")) {
                condition.setCode(getChiefComplaintCode(member.getValueCoded()));
            }
            else if (memberConceptName.equalsIgnoreCase("Chief Complaint Duration")) {
                condition.setOnset(getOnsetDate(member));
            }
        }

        Identifier identifier = condition.addIdentifier();
        identifier.setValueSimple(obs.getUuid());

        return condition;
    }

    private org.hl7.fhir.instance.model.DateTime getOnsetDate(Obs member) {
        Double durationInMinutes = member.getValueNumeric();
        final java.util.Date obsDatetime = member.getObsDatetime();
        org.joda.time.DateTime obsTime = new DateTime(obsDatetime);
        final java.util.Date assertedDateTime = obsTime.minusMinutes(durationInMinutes.intValue()).toDate();
        org.hl7.fhir.instance.model.DateTime assertedDate = new org.hl7.fhir.instance.model.DateTime();
        assertedDate.setValue(new DateAndTime(assertedDateTime));
        return assertedDate;
    }

    private CodeableConcept getChiefComplaintSevirity() {
        CodeableConcept conditionSeverity = new CodeableConcept();
        Coding coding = conditionSeverity.addCoding();
        coding.setDisplaySimple(FHIRProperties.FHIR_SEVERITY_MODERATE);
        coding.setCodeSimple(FHIRProperties.SNOMED_VALUE_MODERATE_SEVERTY);
        coding.setSystemSimple(FHIRProperties.FHIR_CONDITION_SEVERITY_URL);
        return conditionSeverity;
    }

    private Enumeration<Condition.ConditionStatus> getConditionStatus(Obs member) {
        return null;
    }

    private ResourceReference getParticipant(Encounter encounter) {
        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
        if ((participants != null) || participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }

    private CodeableConcept getChiefComplaintCategory() {
        CodeableConcept conditionCategory = new CodeableConcept();
        Coding coding = conditionCategory.addCoding();
        coding.setCodeSimple(FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT);
        coding.setSystemSimple(FHIRProperties.FHIR_CONDITION_CATEGORY_URL);
        coding.setDisplaySimple("Complaint");
        return conditionCategory;
    }

    private class ConceptCoding {
        String code;
        String source;
    }

    private CodeableConcept getChiefComplaintCode(Concept obsConcept) {
        CodeableConcept diagnosisCode = new CodeableConcept();
        Coding coding = diagnosisCode.addCoding();
        //TODO to change to reference term code
        ConceptCoding refCoding = getReferenceCode(obsConcept);
        coding.setCodeSimple(refCoding.code);
        coding.setSystemSimple(refCoding.source);
        coding.setDisplaySimple(obsConcept.getName().getName());
        return diagnosisCode;
    }

    private ConceptCoding getReferenceCode(Concept obsConcept) {
        Collection<org.openmrs.ConceptMap> conceptMappings = obsConcept.getConceptMappings();
        for (org.openmrs.ConceptMap mapping : conceptMappings) {
            //TODO right now returning the first mapping
            ConceptCoding coding = new ConceptCoding();
            ConceptReferenceTerm conceptReferenceTerm = mapping.getConceptReferenceTerm();
            coding.code = conceptReferenceTerm.getCode();
            coding.source = conceptReferenceTerm.getConceptSource().getName();
            return coding;
        }
        ConceptCoding defaultCoding = new ConceptCoding();
        defaultCoding.code = obsConcept.getUuid();
        //TODO: put in the right URL. To be mapped
        defaultCoding.source = Constants.TERMINOLOGY_SERVER_CONCEPT_URL + obsConcept.getUuid();
        return defaultCoding;
    }
}
