package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Condition;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class FHIRDiagnosisConditionsMapper {

    private final Map<Condition.ConditionStatus, String> diaConditionStatus = new HashMap<Condition.ConditionStatus, String>();
    @Autowired
    private ConceptService conceptService;

    private final Map<String,String> diaConditionSeverity = new HashMap<String, String>();

    public FHIRDiagnosisConditionsMapper() {
        diaConditionSeverity.put("Moderate", "Primary");
        diaConditionSeverity.put("Severe", "Secondary");

        diaConditionStatus.put(Condition.ConditionStatus.provisional, "Presumed");
        diaConditionStatus.put(Condition.ConditionStatus.confirmed, "Confirmed");
    }

    public void map(Patient emrPatient, Encounter newEmrEncounter, Condition condition) {


            Obs visitDiagnosisObs = new Obs();

            Concept diagnosisOrder = conceptService.getConceptByName("Diagnosis order");
            Concept diagnosisCertainty = conceptService.getConceptByName("Diagnosis Certainty");
            Concept codedDiagnosis = conceptService.getConceptByName("Coded Diagnosis");
            Concept visitDiagnosis = conceptService.getConceptByName("Visit Diagnoses");

            Concept bahmniInitialDiagnosis = conceptService.getConceptByName("Bahmni Initial Diagnosis");
            Concept bahmniDiagnosisStatus = conceptService.getConceptByName("Bahmni Diagnosis Status");
            Concept bahmniDiagnosisRevised = conceptService.getConceptByName("Bahmni Diagnosis Revised");

            Concept diagnosisConceptAnswer = identifyDiagnosisConcept(condition);
            Concept diagnosisSeverityAnswer = identifyDiagnosisSeverity(condition, diagnosisOrder);
            Concept diagnosisCertaintyAnswer = identifyDiagnosisCertainty(condition, diagnosisCertainty);

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
                if (answer.getAnswerConcept().getName().getName().equals("Confirmed")) {
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

                if (severityAnswerConcept==null) {
                    for (ConceptAnswer answer : answers) {
                        if (answer.getAnswerConcept().getName().getName().equals("Primary")) {
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

    private Concept identifyDiagnosisConcept(Condition condition) {
        CodeableConcept codeableConcept = condition.getCode();
        List<Coding> codeList = codeableConcept.getCoding();

        if ((codeList != null) && !codeList.isEmpty()) {
            Coding coding = codeList.get(0);
            String diagnosisCode = coding.getCodeSimple();
            String systemSimple = coding.getSystemSimple();
            String diagnosisName = coding.getDisplaySimple();
            if (systemSimple.startsWith("ICD10")) {
                ConceptSource conceptSource = conceptService.getConceptSourceByName(systemSimple);
                //ConceptReferenceTerm referenceTerm = conceptService.getConceptReferenceTermByCode(diagnosisCode, conceptSource);
                List<Concept> conceptsByMapping = conceptService.getConceptsByMapping(diagnosisCode, conceptSource.getName());
                if ( (conceptsByMapping != null) && !conceptsByMapping.isEmpty()) {
                    Concept mappedConcept = null;
                    for (Concept concept : conceptsByMapping) {
                        if (concept.getName().getName().equalsIgnoreCase(diagnosisName)) {
                            return concept;
                        }
                    }
                    if (mappedConcept == null) {
                        return conceptsByMapping.get(0);
                    }
                } else {
                   return null;
                }

            } else {
                return conceptService.getConceptByName(diagnosisName);
            }

        }
        return null;
    }
}
