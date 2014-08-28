package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Date;
import org.hl7.fhir.instance.model.Enumeration;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("fhirDiagnosisMapper")
public class DiagnosisMapper implements EmrResourceHandler {

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    private final Map<String, Condition.ConditionStatus> diaConditionStatus = new HashMap<String, Condition.ConditionStatus>();
    private final FHIRProperties fhirProperties;

    public DiagnosisMapper() {
        fhirProperties = new FHIRProperties();
        diaConditionStatus.put(MRSProperties.MRS_DIAGNOSIS_STATUS_PRESUMED, Condition.ConditionStatus.provisional);
        diaConditionStatus.put(MRSProperties.MRS_DIAGNOSIS_STATUS_CONFIRMED, Condition.ConditionStatus.confirmed);
    }

    @Override
    public boolean handles(Obs observation) {
        return observation.getConcept().getName().getName().equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_VISIT_DIAGNOSES);
    }

    @Override
    public List<EmrResource> map(Obs obs, Encounter fhirEncounter) {
        List<EmrResource> diagnoses = new ArrayList<EmrResource>();
        EmrResource fhirCondition = createFHIRCondition(fhirEncounter, obs);
        if (fhirCondition != null) {
            diagnoses.add(fhirCondition);
        }
        return diagnoses;
    }

    private EmrResource createFHIRCondition(Encounter encounter, Obs obs) {
        Condition condition = new Condition();
        condition.setEncounter(encounter.getIndication());
        condition.setSubject(encounter.getSubject());
        ResourceReference participant = getParticipant(encounter);
        if (null != participant) {
            condition.setAsserter(participant);
        }
        condition.setCategory(getDiagnosisCategory());

        final Set<Obs> obsMembers = obs.getGroupMembers(false);
        for (Obs member : obsMembers) {
            Concept memberConcept = member.getConcept();
            if (isCodedDiagnosisObservation(memberConcept)) {
                CodeableConcept diagnosisCode = FHIRFeedHelper.addReferenceCodes(member.getValueCoded(), idMappingsRepository);
                if (CollectionUtils.isEmpty(diagnosisCode.getCoding())) {
                    return null;
                }
                condition.setCode(diagnosisCode);
            } else if (isDiagnosisCertaintyObservation(memberConcept)) {
                condition.setStatus(getConditionStatus(member));
            }
        }

        Date onsetDate = new Date();
        onsetDate.setValue(new DateAndTime(obs.getObsDatetime()));
        condition.setDateAsserted(onsetDate);

        Identifier identifier = condition.addIdentifier();
        identifier.setValueSimple(obs.getUuid());

        if (CollectionUtils.isEmpty(condition.getCode().getCoding())) {
            return null;
        }
        return new EmrResource("Diagnosis", condition.getIdentifier(), condition);
    }

    private boolean isDiagnosisCertaintyObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_CERTAINTY);
    }

    private boolean isCodedDiagnosisObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_CODED_DIAGNOSIS);
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

    private CodeableConcept getDiagnosisCategory() {
        return FHIRFeedHelper.getFHIRCodeableConcept(FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS,
                FHIRProperties.FHIR_CONDITION_CATEGORY_URL, FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS);
    }
}
