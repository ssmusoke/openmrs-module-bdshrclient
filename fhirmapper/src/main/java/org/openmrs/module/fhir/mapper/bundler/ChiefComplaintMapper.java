package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.joda.time.DateTime;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.openmrs.module.fhir.mapper.model.ObservationType.CHIEF_COMPLAINT_DATA;
import static org.openmrs.module.fhir.mapper.model.ObservationType.HISTORY_AND_EXAMINATION;

@Component
public class ChiefComplaintMapper implements EmrObsResourceHandler {

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Override
    public boolean handles(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        if (obs.isOfType(HISTORY_AND_EXAMINATION)) {
            if (obs.findMember(CHIEF_COMPLAINT_DATA) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<EmrResource> map(Obs obs, Encounter fhirEncounter) {
        List<EmrResource> chiefComplaints = new ArrayList<EmrResource>();
        for (Obs member : obs.getGroupMembers()) {
            if (member.getConcept().getName().getName().equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DATA)) {
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
        condition.setStatus(new Enumeration<Condition.ConditionStatus>(Condition.ConditionStatus.confirmed));

        org.hl7.fhir.instance.model.Date assertedDate = new org.hl7.fhir.instance.model.Date();
        assertedDate.setValue(new DateAndTime(obs.getObsDatetime()));
        condition.setDateAsserted(assertedDate);

        final Set<Obs> obsMembers = obs.getGroupMembers(false);
        for (Obs member : obsMembers) {
            final String memberConceptName = member.getConcept().getName().getName();
            if (memberConceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT)) {
                final CodeableConcept complaintCode = FHIRFeedHelper.addReferenceCodes(member.getValueCoded(), idMappingsRepository);
                if (CollectionUtils.isEmpty(complaintCode.getCoding())) {
                    Coding coding = complaintCode.addCoding();
                    coding.setDisplaySimple(member.getValueCoded().getName().getName());
                }
                condition.setCode(complaintCode);
            } else if (memberConceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DURATION)) {
                condition.setOnset(getOnsetDate(member));
            } else if (memberConceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_NON_CODED_CHIEF_COMPLAINT)) {
                //TODO : Put right Values
                CodeableConcept nonCodedChiefComplaint = new CodeableConcept();
                Coding coding = nonCodedChiefComplaint.addCoding();
                coding.setDisplaySimple(member.getValueText());
                condition.setCode(nonCodedChiefComplaint);
            }
        }

        Identifier identifier = condition.addIdentifier();
        identifier.setValueSimple(obs.getUuid());

        return new EmrResource(FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT, condition.getIdentifier(), condition);
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

    private CodeableConcept getChiefComplaintCategory() {
        return FHIRFeedHelper.getFHIRCodeableConcept(FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT,
                FHIRProperties.FHIR_CONDITION_CATEGORY_URL, FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT_DISPLAY);
    }

    //TODO : how do we identify this individual?
    protected ResourceReference getParticipant(Encounter encounter) {
        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
        if ((participants != null) && !participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }
}
