package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.valueset.ConditionClinicalStatusEnum;
import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openmrs.module.fhir.mapper.model.ObservationType.VISIT_DIAGNOSES;

@Component("fhirDiagnosisMapper")
public class DiagnosisMapper implements EmrObsResourceHandler {

    @Autowired
    private IdMappingsRepository idMappingsRepository;
    @Autowired
    private CodableConceptService codableConceptService;

    private final Map<String, ConditionClinicalStatusEnum> diaConditionStatus = new HashMap<String, ConditionClinicalStatusEnum>();

    public DiagnosisMapper() {
        diaConditionStatus.put(MRSProperties.MRS_DIAGNOSIS_STATUS_PRESUMED, ConditionClinicalStatusEnum.PROVISIONAL);
        diaConditionStatus.put(MRSProperties.MRS_DIAGNOSIS_STATUS_CONFIRMED, ConditionClinicalStatusEnum.CONFIRMED);
    }

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        return obs.isOfType(VISIT_DIAGNOSES);
    }

    @Override
    public List<FHIRResource> map(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> diagnoses = new ArrayList<>();

        FHIRResource fhirCondition = createFHIRCondition(fhirEncounter, obs, systemProperties);
        if (fhirCondition != null) {
            diagnoses.add(fhirCondition);
        }
        return diagnoses;
    }

    private FHIRResource createFHIRCondition(Encounter encounter, Obs obs, SystemProperties systemProperties) {
        Condition condition = new Condition();
        condition.setEncounter(new ResourceReferenceDt().setReference(encounter.getId().getValueAsString()));
        condition.setPatient(encounter.getPatient());
        ResourceReferenceDt participant = getParticipant(encounter);
        if (null != participant) {
            condition.setAsserter(participant);
        }
        condition.setCategory(getDiagnosisCategory());

        final Set<Obs> obsMembers = obs.getGroupMembers(false);
        for (Obs member : obsMembers) {
            Concept memberConcept = member.getConcept();
            if (isCodedDiagnosisObservation(memberConcept)) {
                CodeableConceptDt diagnosisCode = codableConceptService.addTRCoding(member.getValueCoded(), idMappingsRepository);
                if (CollectionUtils.isEmpty(diagnosisCode.getCoding())) {
                    return null;
                }
                condition.setCode(diagnosisCode);
            } else if (isDiagnosisCertaintyObservation(memberConcept)) {
                condition.setClinicalStatus(getConditionStatus(member));
            }
        }
        if (condition.getCode() == null || CollectionUtils.isEmpty(condition.getCode().getCoding())) {
            return null;
        }

        condition.setDateAsserted(obs.getObsDatetime(), TemporalPrecisionEnum.DAY);
        IdentifierDt identifier = condition.addIdentifier();
        String obsId = new EntityReference().build(IResource.class, systemProperties, obs.getUuid());
        identifier.setValue(obsId);
        condition.setId(obsId);

        return new FHIRResource("Diagnosis", condition.getIdentifier(), condition);
    }

    private boolean isDiagnosisCertaintyObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_CERTAINTY);
    }

    private boolean isCodedDiagnosisObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_CODED_DIAGNOSIS);
    }


    private ConditionClinicalStatusEnum getConditionStatus(Obs member) {
        Concept diagnosisStatus = member.getValueCoded();
        ConditionClinicalStatusEnum status = diaConditionStatus.get(diagnosisStatus.getName().getName());
        return status != null ? status : ConditionClinicalStatusEnum.CONFIRMED;
    }

    private CodeableConceptDt getDiagnosisCategory() {
        return codableConceptService.getFHIRCodeableConcept(FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS,
                FHIRProperties.FHIR_CONDITION_CATEGORY_URL, FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS_DISPLAY);
    }

    protected ResourceReferenceDt getParticipant(Encounter encounter) {
        List<Encounter.Participant> participants = encounter.getParticipant();
        if ((participants != null) && !participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }
}
