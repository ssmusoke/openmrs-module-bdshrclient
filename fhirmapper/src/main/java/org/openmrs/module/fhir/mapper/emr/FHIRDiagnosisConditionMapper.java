package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.valueset.ConditionClinicalStatusEnum;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class FHIRDiagnosisConditionMapper implements FHIRResourceMapper {

    private final Map<ConditionClinicalStatusEnum, String> diaConditionStatus = new HashMap<>();
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private OMRSConceptLookup omrsConceptLookup;


    public FHIRDiagnosisConditionMapper() {
        diaConditionStatus.put(ConditionClinicalStatusEnum.PROVISIONAL, MRSProperties.MRS_DIAGNOSIS_STATUS_PRESUMED);
        diaConditionStatus.put(ConditionClinicalStatusEnum.CONFIRMED, MRSProperties.MRS_DIAGNOSIS_STATUS_CONFIRMED);
    }

    @Override
    public boolean canHandle(IResource resource) {
        if (resource instanceof Condition) {
            final List<CodingDt> resourceCoding = ((Condition) resource).getCategory().getCoding();
            if (resourceCoding == null || resourceCoding.isEmpty()) {
                return false;
            }
            return resourceCoding.get(0).getCode().equalsIgnoreCase(FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS);
        }
        return false;
    }

    @Override
    public void map(Bundle bundle, IResource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {
        Condition condition = (Condition) resource;

        if (isAlreadyProcessed(condition, processedList))
            return;
        Obs visitDiagnosisObs = new Obs();

        Concept diagnosisOrder = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_ORDER);
        Concept diagnosisCertainty = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_CERTAINTY);
        Concept codedDiagnosis = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_CODED_DIAGNOSIS);
        Concept visitDiagnosis = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_VISIT_DIAGNOSES);

        Concept bahmniInitialDiagnosis = conceptService.getConceptByName("Bahmni Initial Diagnosis");
        Concept bahmniDiagnosisStatus = conceptService.getConceptByName("Bahmni Diagnosis Status");
        Concept bahmniDiagnosisRevised = conceptService.getConceptByName("Bahmni Diagnosis Revised");

        Concept diagnosisConceptAnswer = omrsConceptLookup.findConcept(condition.getCode().getCoding());
        Concept diagnosisSeverityAnswer = identifyDiagnosisSeverity(diagnosisOrder);
        Concept diagnosisCertaintyAnswer = identifyDiagnosisCertainty(condition, diagnosisCertainty);

        if (diagnosisConceptAnswer == null) {
            return;
        }

        visitDiagnosisObs.setConcept(visitDiagnosis);
        visitDiagnosisObs.setPerson(emrPatient);

        Obs orderObs = addToObsGroup(emrPatient, visitDiagnosisObs, diagnosisOrder);
        orderObs.setValueCoded(diagnosisSeverityAnswer);

        Obs certaintyObs = addToObsGroup(emrPatient, visitDiagnosisObs, diagnosisCertainty);
        certaintyObs.setValueCoded(diagnosisCertaintyAnswer);

        Obs codedObs = addToObsGroup(emrPatient, visitDiagnosisObs, codedDiagnosis);
        codedObs.setValueCoded(diagnosisConceptAnswer);

        Obs bahmniInitDiagObs = addToObsGroup(emrPatient, visitDiagnosisObs, bahmniInitialDiagnosis);
        bahmniInitDiagObs.setValueText(visitDiagnosisObs.getUuid());

        Obs bahmniDiagStatusObs = addToObsGroup(emrPatient, visitDiagnosisObs, bahmniDiagnosisStatus);
        bahmniDiagStatusObs.setValueBoolean(false);

        Obs bahmniDiagRevisedObs = addToObsGroup(emrPatient, visitDiagnosisObs, bahmniDiagnosisRevised);
        bahmniDiagRevisedObs.setValueBoolean(false);

        newEmrEncounter.addObs(visitDiagnosisObs);

        processedList.put(condition.getIdentifier().get(0).getValue(), Arrays.asList(visitDiagnosisObs.getUuid()));
    }


    private boolean isAlreadyProcessed(Condition condition, Map<String, List<String>> processedList) {
        return processedList.containsKey(condition.getIdentifier().get(0).getValue());
    }

    private Obs addToObsGroup(Patient emrPatient, Obs visitDiagnosisObs, Concept obsConcept) {
        Obs orderObs = new Obs();
        orderObs.setConcept(obsConcept);
        orderObs.setPerson(emrPatient);
        visitDiagnosisObs.addGroupMember(orderObs);
        return orderObs;
    }

    private Concept identifyDiagnosisCertainty(Condition condition, Concept diagnosisCertainty) {
        ConditionClinicalStatusEnum conditionStatus = condition.getClinicalStatusElement().getValueAsEnum();
        String status = diaConditionStatus.get(conditionStatus);

        Concept certaintyAnswerConcept = null;

        Collection<ConceptAnswer> answers = diagnosisCertainty.getAnswers();
        for (ConceptAnswer answer : answers) {
            if (answer.getAnswerConcept().getName().getName().equalsIgnoreCase(status)) {
                certaintyAnswerConcept = answer.getAnswerConcept();
            }
        }
        if (certaintyAnswerConcept == null) {
            for (ConceptAnswer answer : answers) {
                if (answer.getAnswerConcept().getName().getName().equals(MRSProperties.MRS_DIAGNOSIS_STATUS_CONFIRMED)) {
                    certaintyAnswerConcept = answer.getAnswerConcept();
                    break;
                }
            }
        }
        return certaintyAnswerConcept;

    }

    private Concept identifyDiagnosisSeverity(Concept diagnosisOrder) {
        Concept severityAnswerConcept = null;
        Collection<ConceptAnswer> answers = diagnosisOrder.getAnswers();
        for (ConceptAnswer answer : answers) {
            if (answer.getAnswerConcept().getName().getName().equals(MRSProperties.MRS_DIAGNOSIS_SEVERITY_PRIMARY)) {
                severityAnswerConcept = answer.getAnswerConcept();
                break;
            }
        }
        return severityAnswerConcept;
    }
}
