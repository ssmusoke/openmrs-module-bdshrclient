package org.bahmni.module.shrclient.mapper;

import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.Enumeration;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class FHIRConditionsMapper {

    private final Map<Condition.ConditionStatus, String> diaConditionStatus = new HashMap<Condition.ConditionStatus, String>();
    @Autowired
    private ConceptService conceptService;

    private final Map<String,String> diaConditionSeverity = new HashMap<String, String>();

    public FHIRConditionsMapper() {
        diaConditionSeverity.put("Moderate", "Primary");
        diaConditionSeverity.put("Severe", "Secondary");

        diaConditionStatus.put(Condition.ConditionStatus.provisional, "Presumed");
        diaConditionStatus.put(Condition.ConditionStatus.confirmed, "Confirmed");
    }

    public void map(Patient emrPatient, Encounter newEmrEncounter, List<Condition> conditions) {
        for (Condition condition : conditions) {

            Obs visitDiagnosisObs = new Obs();

            Concept diagnosisOrder = conceptService.getConceptByName("Diagnosis order");
            Concept diagnosisCertainty = conceptService.getConceptByName("Diagnosis Certainty");
            Concept codedDiagnosis = conceptService.getConceptByName("Coded Diagnosis");
            Concept visitDiagnosis = conceptService.getConceptByName("Visit Diagnoses");

            Concept bahmniInitialDiagnosis = conceptService.getConceptByName("Bahmni Initial Diagnosis");
            Concept bahmniDiagnosisStatus = conceptService.getConceptByName("Bahmni Diagnosis Status");
            Concept bahmniDiagnosisRevised = conceptService.getConceptByName("Bahmni Diagnosis Revised");

            Concept falseConcept = conceptService.getConceptByName("False");




            Concept diagnosisConceptAnswer = identifyDiagnosisConcept(condition);
            Concept diagnosisSeverityAnswer = identifyDiagnosisSeverity(condition, diagnosisOrder);
            Concept diagnosisCertaintyAnswer = identifyDiagnosisCertainty(condition, diagnosisCertainty);

            visitDiagnosisObs.setConcept(visitDiagnosis);


            Obs orderObs = new Obs();
            orderObs.setConcept(diagnosisOrder);
            orderObs.setPerson(emrPatient);
            orderObs.setValueCoded(diagnosisSeverityAnswer);
            visitDiagnosisObs.addGroupMember(orderObs);

            Obs certaintyObs = new Obs();
            certaintyObs.setConcept(diagnosisCertainty);
            certaintyObs.setPerson(emrPatient);
            certaintyObs.setValueCoded(diagnosisCertaintyAnswer);
            visitDiagnosisObs.addGroupMember(certaintyObs);

            Obs codedObs = new Obs();
            codedObs.setConcept(codedDiagnosis);
            codedObs.setPerson(emrPatient);
            codedObs.setValueCoded(diagnosisConceptAnswer);
            visitDiagnosisObs.addGroupMember(codedObs);


            Obs bahmniInitDiagObs = new Obs();
            bahmniInitDiagObs.setConcept(bahmniInitialDiagnosis);
            bahmniInitDiagObs.setPerson(emrPatient);
            bahmniInitDiagObs.setValueText(visitDiagnosisObs.getUuid());
            visitDiagnosisObs.addGroupMember(bahmniInitDiagObs);

            Obs bahmniDiagStatusObs = new Obs();
            bahmniDiagStatusObs.setConcept(bahmniDiagnosisStatus);
            bahmniDiagStatusObs.setValueCoded(falseConcept);
            bahmniDiagStatusObs.setPerson(emrPatient);
            visitDiagnosisObs.addGroupMember(bahmniDiagStatusObs);

            Obs bahmniDiagRevisedObs = new Obs();
            bahmniDiagRevisedObs.setConcept(bahmniDiagnosisRevised);
            bahmniDiagRevisedObs.setValueBoolean(false);
            bahmniDiagRevisedObs.setPerson(emrPatient);
            visitDiagnosisObs.addGroupMember(bahmniDiagRevisedObs);

            newEmrEncounter.addObs(visitDiagnosisObs);
        }


    }

    private Concept identifyDiagnosisCertainty(Condition condition, Concept diagnosisCertainty) {
        Enumeration<Condition.ConditionStatus> conditionStatus = condition.getStatus();
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
            Concept diagnosisConcept = null;
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
