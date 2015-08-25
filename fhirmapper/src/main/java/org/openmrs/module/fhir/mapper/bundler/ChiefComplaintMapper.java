package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.valueset.ConditionClinicalStatusEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.CodableConceptService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
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
    private IdMappingsRepository idMappingsRepository;
    @Autowired
    private CodableConceptService codableConceptService;

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        return obs.isOfType(COMPLAINT_CONDITION_TEMPLATE);
    }

    @Override
    public List<FHIRResource> map(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> chiefComplaints = new ArrayList<>();
        CompoundObservation conditionTemplateObs = new CompoundObservation(obs);
        List<Obs> chiefComplaintDataObsList = conditionTemplateObs.findAllMemberObsForConceptName(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DATA);
        for (Obs chiefComplaintDataObs : chiefComplaintDataObsList) {
                chiefComplaints.add(createFHIRCondition(fhirEncounter, chiefComplaintDataObs, systemProperties));
        }
        return chiefComplaints;
    }

    private FHIRResource createFHIRCondition(Encounter encounter, Obs obs, SystemProperties systemProperties) {
        Condition condition = new Condition();
        condition.setEncounter(new ResourceReferenceDt().setReference(encounter.getId().getValue()));
        condition.setPatient(encounter.getPatient());
        condition.setAsserter(getParticipant(encounter));
        condition.setCategory(getChiefComplaintCategory());
        condition.setClinicalStatus(ConditionClinicalStatusEnum.CONFIRMED);
        condition.setDateAsserted(obs.getObsDatetime(), TemporalPrecisionEnum.DAY);

        final Set<Obs> obsMembers = obs.getGroupMembers(false);
        for (Obs member : obsMembers) {
            final String memberConceptName = member.getConcept().getName().getName();
            if (memberConceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT)) {
                final CodeableConceptDt complaintCode = codableConceptService.addTRCoding(member.getValueCoded(), idMappingsRepository);
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

        return new FHIRResource(FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT, condition.getIdentifier(), condition);
    }

    private DateTimeDt getOnsetDate(Obs member) {
        Double durationInMinutes = member.getValueNumeric();
        final java.util.Date obsDatetime = member.getObsDatetime();
        org.joda.time.DateTime obsTime = new DateTime(obsDatetime);
        final java.util.Date assertedDateTime = obsTime.minusMinutes(durationInMinutes.intValue()).toDate();
        return new DateTimeDt(assertedDateTime, TemporalPrecisionEnum.MILLI);
    }

    private CodeableConceptDt getChiefComplaintCategory() {
        return codableConceptService.getFHIRCodeableConcept(FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT,
                FHIRProperties.FHIR_CONDITION_CATEGORY_URL, FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT_DISPLAY);
    }

    protected ResourceReferenceDt getParticipant(Encounter encounter) {
        List<Encounter.Participant> participants = encounter.getParticipant();
        if ((participants != null) && !participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }
}
