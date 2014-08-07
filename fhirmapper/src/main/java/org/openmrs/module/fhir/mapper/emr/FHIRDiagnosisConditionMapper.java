package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.Resource;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.utils.OMRSHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class FHIRDiagnosisConditionMapper implements FHIRResource {

    private final Map<Condition.ConditionStatus, String> diaConditionStatus = new HashMap<Condition.ConditionStatus, String>();
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private OMRSHelper omrsHelper;

    private final Map<String, String> diaConditionSeverity = new HashMap<String, String>();

    public FHIRDiagnosisConditionMapper() {
        diaConditionSeverity.put("Moderate", MRSProperties.MRS_DIAGNOSIS_SEVERITY_PRIMARY);
        diaConditionSeverity.put("Severe", MRSProperties.MRS_DIAGNOSIS_SEVERITY_SECONDARY);

        diaConditionStatus.put(Condition.ConditionStatus.provisional, MRSProperties.MRS_DIAGNOSIS_STATUS_PRESUMED);
        diaConditionStatus.put(Condition.ConditionStatus.confirmed, MRSProperties.MRS_DIAGNOSIS_STATUS_CONFIRMED);
    }

    @Override
    public boolean handles(Resource resource) {
        if (resource instanceof Condition) {
            final List<Coding> resourceCoding = ((Condition) resource).getCategory().getCoding();
            if (resourceCoding == null || resourceCoding.isEmpty()) {
                return false;
            }
            return resourceCoding.get(0).getCodeSimple().equalsIgnoreCase(FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS);
        }
        return false;
    }

    @Override
    public void map(Resource resource, Patient emrPatient, Encounter newEmrEncounter) {
        Condition condition = (Condition) resource;

        Obs visitDiagnosisObs = new Obs();

        Concept diagnosisOrder = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_ORDER);
        Concept diagnosisCertainty = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_CERTAINTY);
        Concept codedDiagnosis = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_CODED_DIAGNOSIS);
        Concept visitDiagnosis = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_VISIT_DIAGNOSES);

        Concept bahmniInitialDiagnosis = conceptService.getConceptByName("Bahmni Initial Diagnosis");
        Concept bahmniDiagnosisStatus = conceptService.getConceptByName("Bahmni Diagnosis Status");
        Concept bahmniDiagnosisRevised = conceptService.getConceptByName("Bahmni Diagnosis Revised");

        Concept diagnosisConceptAnswer = omrsHelper.findConcept(condition.getCode().getCoding());
        Concept diagnosisSeverityAnswer = identifyDiagnosisSeverity(condition, diagnosisOrder);
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
    }

    private Obs addToObsGroup(Patient emrPatient, Obs visitDiagnosisObs, Concept obsConcept) {
        Obs orderObs = new Obs();
        orderObs.setConcept(obsConcept);
        orderObs.setPerson(emrPatient);
        visitDiagnosisObs.addGroupMember(orderObs);
        return orderObs;
    }

    private Concept identifyDiagnosisCertainty(Condition condition, Concept diagnosisCertainty) {
        Condition.ConditionStatus conditionStatus = condition.getStatus().getValue();
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

    private Concept identifyDiagnosisSeverity(Condition condition, Concept diagnosisOrder) {
        CodeableConcept severity = condition.getSeverity();
        if (severity != null) {
            List<Coding> severityCodeList = severity.getCoding();
            if ((severityCodeList != null) && !severityCodeList.isEmpty()) {
                String severityText = severityCodeList.get(0).getDisplaySimple();
                String severityAnswer = diaConditionSeverity.get(severityText);
                Concept severityAnswerConcept = null;
                Collection<ConceptAnswer> answers = diagnosisOrder.getAnswers();
                for (ConceptAnswer answer : answers) {
                    if (answer.getAnswerConcept().getName().getName().equals(severityAnswer)) {
                        severityAnswerConcept = answer.getAnswerConcept();
                        break;
                    }
                }

                if (severityAnswerConcept == null) {
                    for (ConceptAnswer answer : answers) {
                        if (answer.getAnswerConcept().getName().getName().equals(MRSProperties.MRS_DIAGNOSIS_SEVERITY_PRIMARY)) {
                            severityAnswerConcept = answer.getAnswerConcept();
                            break;
                        }
                    }
                }
                return severityAnswerConcept;
            }
        }
        return null;
    }
}
