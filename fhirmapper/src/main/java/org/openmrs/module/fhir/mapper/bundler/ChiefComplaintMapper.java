package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.DateAndTime;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Enumeration;
import org.hl7.fhir.instance.model.Identifier;
import org.hl7.fhir.instance.model.ResourceReference;
import org.joda.time.DateTime;
import org.openmrs.Concept;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.utils.ConceptCoding;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class ChiefComplaintMapper implements EmrResourceHandler{

    @Override
    public boolean handles(Obs observation) {
        if(observation.getConcept().getName().getName().equalsIgnoreCase("History and Examination")) {
            for (Obs member : observation.getGroupMembers()) {
                if(member.getConcept().getName().getName().equalsIgnoreCase("Chief Complaint Data")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<EmrResource> map(Obs obs, Encounter fhirEncounter) {
        List<EmrResource> chiefComplaints = new ArrayList<EmrResource>();
        for (Obs member : obs.getGroupMembers()) {
            if (member.getConcept().getName().getName().equalsIgnoreCase("Chief Complaint Data")) {
                chiefComplaints.add(createFHIRCondition(fhirEncounter, member));
            }
        }
        return chiefComplaints;
    }

    private EmrResource createFHIRCondition(Encounter encounter, Obs obs) {
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
            else if(memberConceptName.equalsIgnoreCase("Non-Coded Chief Complaint")) {
                //TODO : Put right Values
                final CodeableConcept nonCodedChiefComplaint = FHIRFeedHelper.getFHIRCodeableConcept(member.getValueText(),
                        Constants.TERMINOLOGY_SERVER_CONCEPT_URL + member.getValueText(),
                        member.getValueText());
                condition.setCode(nonCodedChiefComplaint);
            }
        }

        Identifier identifier = condition.addIdentifier();
        identifier.setValueSimple(obs.getUuid());

        return new EmrResource("Complaint", condition.getIdentifier(), condition);
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
        return FHIRFeedHelper.getFHIRCodeableConcept(FHIRProperties.SNOMED_VALUE_MODERATE_SEVERTY,
                FHIRProperties.FHIR_CONDITION_SEVERITY_URL, FHIRProperties.FHIR_SEVERITY_MODERATE);
    }

    private ResourceReference getParticipant(Encounter encounter) {
        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
        if ((participants != null) && !participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }

    private CodeableConcept getChiefComplaintCategory() {
        CodeableConcept conditionCategory = FHIRFeedHelper.getFHIRCodeableConcept(FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT,
                FHIRProperties.FHIR_CONDITION_CATEGORY_URL, "Complaint");
        return conditionCategory;
    }


    private CodeableConcept getChiefComplaintCode(Concept obsConcept) {
        //TODO to change to reference term code
        ConceptCoding refCoding = getReferenceCode(obsConcept);
        final String conceptName = obsConcept.getName().getName();
        return FHIRFeedHelper.getFHIRCodeableConcept(refCoding.getCode(), refCoding.getSource(), conceptName);
    }

    private ConceptCoding getReferenceCode(Concept obsConcept) {
        Collection<org.openmrs.ConceptMap> conceptMappings = obsConcept.getConceptMappings();
        for (org.openmrs.ConceptMap mapping : conceptMappings) {
            //TODO right now returning the first mapping
            ConceptReferenceTerm conceptReferenceTerm = mapping.getConceptReferenceTerm();
            //TODO right now returning the first mapping
            ConceptCoding coding = new ConceptCoding();
            coding.setCode(conceptReferenceTerm.getCode());
            coding.setSource(conceptReferenceTerm.getConceptSource().getName());
            return coding;
        }
        ConceptCoding defaultCoding = new ConceptCoding();
        defaultCoding.setCode(obsConcept.getUuid());
        //TODO: put in the right URL. To be mapped
        defaultCoding.setSource(Constants.TERMINOLOGY_SERVER_CONCEPT_URL + obsConcept.getUuid());
        return defaultCoding;
    }
}
