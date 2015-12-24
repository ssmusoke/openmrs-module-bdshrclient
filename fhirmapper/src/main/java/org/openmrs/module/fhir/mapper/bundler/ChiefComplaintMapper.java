package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.valueset.ConditionCategoryCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionClinicalStatusCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionVerificationStatusEnum;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.openmrs.Obs;
import org.openmrs.module.fhir.FHIRProperties;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.module.fhir.utils.CodeableConceptService;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.openmrs.module.fhir.mapper.model.ObservationType.COMPLAINT_CONDITION_TEMPLATE;

@Component
public class ChiefComplaintMapper implements EmrObsResourceHandler {

    @Autowired
    private CodeableConceptService codeableConceptService;

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        return obs.isOfType(COMPLAINT_CONDITION_TEMPLATE);
    }

    @Override
    public List<FHIRResource> map(Obs obs, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> chiefComplaints = new ArrayList<>();
        CompoundObservation conditionTemplateObs = new CompoundObservation(obs);
        List<Obs> chiefComplaintDataObsList = conditionTemplateObs.getAllMemberObsForConceptName(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DATA);
        for (Obs chiefComplaintDataObs : chiefComplaintDataObsList) {
            chiefComplaints.add(createFHIRCondition(fhirEncounter, chiefComplaintDataObs, systemProperties));
        }
        return chiefComplaints;
    }

    private FHIRResource createFHIRCondition(FHIREncounter fhirEncounter, Obs obs, SystemProperties systemProperties) {
        Condition condition = new Condition();
        condition.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId()));
        condition.setPatient(fhirEncounter.getPatient());
        condition.setAsserter(fhirEncounter.getFirstParticipantReference());
        condition.setCategory(ConditionCategoryCodesEnum.COMPLAINT);
        condition.setClinicalStatus(ConditionClinicalStatusCodesEnum.ACTIVE);
        condition.setVerificationStatus(ConditionVerificationStatusEnum.PROVISIONAL);

        final Set<Obs> obsMembers = obs.getGroupMembers(false);
        for (Obs member : obsMembers) {
            final String memberConceptName = member.getConcept().getName().getName();
            if (memberConceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT)) {
                final CodeableConceptDt complaintCode = codeableConceptService.addTRCoding(member.getValueCoded());
                if (CollectionUtils.isEmpty(complaintCode.getCoding())) {
                    CodingDt coding = complaintCode.addCoding();
                    coding.setDisplay(member.getValueCoded().getName().getName());
                }
                condition.setCode(complaintCode);
            } else if (memberConceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DURATION)) {
                condition.setOnset(getOnsetDate(member));
            } else if (memberConceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_NON_CODED_CHIEF_COMPLAINT)) {
                CodeableConceptDt nonCodedChiefComplaintCode = new CodeableConceptDt();
                CodingDt coding = nonCodedChiefComplaintCode.addCoding();
                coding.setDisplay(member.getValueText());
                condition.setCode(nonCodedChiefComplaintCode);
            }
        }

        IdentifierDt identifier = condition.addIdentifier();
        String conditionId = new EntityReference().build(Obs.class, systemProperties, obs.getUuid());
        identifier.setValue(conditionId);
        condition.setId(conditionId);

        return new FHIRResource(FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT_DISPLAY, condition.getIdentifier(), condition);
    }

    private PeriodDt getOnsetDate(Obs member) {
        Double durationInMinutes = member.getValueNumeric();
        final java.util.Date obsDatetime = member.getObsDatetime();
        org.joda.time.DateTime obsTime = new DateTime(obsDatetime);
        final java.util.Date assertedDateTime = obsTime.minusMinutes(durationInMinutes.intValue()).toDate();
        PeriodDt periodDt = new PeriodDt();
        periodDt.setStart(assertedDateTime, TemporalPrecisionEnum.MILLI);
        periodDt.setEnd(obsDatetime, TemporalPrecisionEnum.MILLI);
        return periodDt;
    }
}
