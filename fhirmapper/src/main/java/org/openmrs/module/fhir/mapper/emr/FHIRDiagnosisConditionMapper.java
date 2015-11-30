package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.valueset.ConditionVerificationStatusEnum;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.FHIRProperties;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.EmrEncounter;
import org.openmrs.module.fhir.mapper.model.ShrEncounter;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class FHIRDiagnosisConditionMapper implements FHIRResourceMapper {

    private final Map<ConditionVerificationStatusEnum, String> diaConditionStatus = new HashMap<>();
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private OMRSConceptLookup omrsConceptLookup;


    public FHIRDiagnosisConditionMapper() {
        diaConditionStatus.put(ConditionVerificationStatusEnum.PROVISIONAL, MRSProperties.MRS_DIAGNOSIS_STATUS_PRESUMED);
        diaConditionStatus.put(ConditionVerificationStatusEnum.CONFIRMED, MRSProperties.MRS_DIAGNOSIS_STATUS_CONFIRMED);
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
    public void map(IResource resource, EmrEncounter emrEncounter, ShrEncounter encounterComposition, SystemProperties systemProperties) {
        Condition condition = (Condition) resource;

        Obs visitDiagnosisObs = new Obs();

        Concept diagnosisOrder = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_ORDER);
        Concept diagnosisCertainty = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_CERTAINTY);
        Concept codedDiagnosis = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_CODED_DIAGNOSIS);
        Concept visitDiagnosis = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_VISIT_DIAGNOSES);

        Concept bahmniInitialDiagnosis = conceptService.getConceptByName("Bahmni Initial Diagnosis");
        Concept bahmniDiagnosisStatus = conceptService.getConceptByName("Bahmni Diagnosis Status");
        Concept bahmniDiagnosisRevised = conceptService.getConceptByName("Bahmni Diagnosis Revised");

        Concept diagnosisConceptAnswer = omrsConceptLookup.findConceptByCode(condition.getCode().getCoding());
        Concept diagnosisSeverityAnswer = identifyDiagnosisSeverity(diagnosisOrder);
        Concept diagnosisCertaintyAnswer = identifyDiagnosisCertainty(condition, diagnosisCertainty);

        if (diagnosisConceptAnswer == null) {
            return;
        }

        visitDiagnosisObs.setConcept(visitDiagnosis);

        Obs orderObs = addToObsGroup(visitDiagnosisObs, diagnosisOrder);
        orderObs.setValueCoded(diagnosisSeverityAnswer);

        Obs certaintyObs = addToObsGroup(visitDiagnosisObs, diagnosisCertainty);
        certaintyObs.setValueCoded(diagnosisCertaintyAnswer);

        Obs codedObs = addToObsGroup(visitDiagnosisObs, codedDiagnosis);
        codedObs.setValueCoded(diagnosisConceptAnswer);

        Obs bahmniInitDiagObs = addToObsGroup(visitDiagnosisObs, bahmniInitialDiagnosis);
        bahmniInitDiagObs.setValueText(visitDiagnosisObs.getUuid());

        Obs bahmniDiagStatusObs = addToObsGroup(visitDiagnosisObs, bahmniDiagnosisStatus);
        bahmniDiagStatusObs.setValueBoolean(false);

        Obs bahmniDiagRevisedObs = addToObsGroup(visitDiagnosisObs, bahmniDiagnosisRevised);
        bahmniDiagRevisedObs.setValueBoolean(false);

        emrEncounter.addObs(visitDiagnosisObs);
    }


    private Obs addToObsGroup(Obs visitDiagnosisObs, Concept obsConcept) {
        Obs orderObs = new Obs();
        orderObs.setConcept(obsConcept);
        visitDiagnosisObs.addGroupMember(orderObs);
        return orderObs;
    }

    private Concept identifyDiagnosisCertainty(Condition condition, Concept diagnosisCertainty) {
        ConditionVerificationStatusEnum conditionStatus = condition.getVerificationStatusElement().getValueAsEnum();
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
