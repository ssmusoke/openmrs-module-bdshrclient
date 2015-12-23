package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.valueset.ConditionCategoryCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionVerificationStatusEnum;
import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
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

import java.util.*;

import static org.openmrs.module.fhir.mapper.model.ObservationType.VISIT_DIAGNOSES;

@Component("fhirDiagnosisMapper")
public class DiagnosisMapper implements EmrObsResourceHandler {

    @Autowired
    private CodeableConceptService codeableConceptService;

    private final Map<String, ConditionVerificationStatusEnum> diaConditionStatus = new HashMap<>();

    public DiagnosisMapper() {
        diaConditionStatus.put(MRSProperties.MRS_DIAGNOSIS_STATUS_PRESUMED, ConditionVerificationStatusEnum.PROVISIONAL);
        diaConditionStatus.put(MRSProperties.MRS_DIAGNOSIS_STATUS_CONFIRMED, ConditionVerificationStatusEnum.CONFIRMED);
    }

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        return obs.isOfType(VISIT_DIAGNOSES);
    }

    @Override
    public List<FHIRResource> map(Obs obs, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> diagnoses = new ArrayList<>();

        FHIRResource fhirCondition = createFHIRCondition(obs, fhirEncounter, systemProperties);
        if (fhirCondition != null) {
            diagnoses.add(fhirCondition);
        }
        return diagnoses;
    }

    private FHIRResource createFHIRCondition(Obs visitDiagnosisObs, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        Condition condition = new Condition();
        condition.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId()));
        condition.setPatient(fhirEncounter.getPatient());
        ResourceReferenceDt participant = fhirEncounter.getFirstParticipantReference();
        if (null != participant) {
            condition.setAsserter(participant);
        }
        condition.setCategory(ConditionCategoryCodesEnum.DIAGNOSIS);

        final Set<Obs> obsMembers = visitDiagnosisObs.getGroupMembers(false);
        for (Obs member : obsMembers) {
            Concept memberConcept = member.getConcept();
            if (isCodedDiagnosisObservation(memberConcept)) {
                CodeableConceptDt diagnosisCode = codeableConceptService.addTRCoding(member.getValueCoded());
                if (CollectionUtils.isEmpty(diagnosisCode.getCoding())) {
                    return null;
                }
                condition.setCode(diagnosisCode);
            } else if (isDiagnosisCertaintyObservation(memberConcept)) {
                condition.setVerificationStatus(getConditionStatus(member));
            }
        }
        if (condition.getCode() == null || CollectionUtils.isEmpty(condition.getCode().getCoding())) {
            return null;
        }

        IdentifierDt identifier = condition.addIdentifier();
        String obsId = new EntityReference().build(IResource.class, systemProperties, visitDiagnosisObs.getUuid());
        identifier.setValue(obsId);
        condition.setId(obsId);
        condition.setNotes(visitDiagnosisObs.getComment());
        return new FHIRResource(FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS_DISPLAY, condition.getIdentifier(), condition);
    }

    private boolean isDiagnosisCertaintyObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_CERTAINTY);
    }

    private boolean isCodedDiagnosisObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_CODED_DIAGNOSIS);
    }


    private ConditionVerificationStatusEnum getConditionStatus(Obs member) {
        Concept diagnosisStatus = member.getValueCoded();
        ConditionVerificationStatusEnum status = diaConditionStatus.get(diagnosisStatus.getName().getName());
        return status != null ? status : ConditionVerificationStatusEnum.CONFIRMED;
    }
}
