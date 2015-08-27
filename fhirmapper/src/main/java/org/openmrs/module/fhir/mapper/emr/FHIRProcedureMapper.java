package org.openmrs.module.fhir.mapper.emr;


import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Procedure;
import org.apache.commons.collections4.CollectionUtils;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.openmrs.module.fhir.mapper.MRSProperties.*;

@Component
public class FHIRProcedureMapper implements FHIRResourceMapper {

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    @Override
    public boolean canHandle(IResource resource) {
        return resource instanceof Procedure;
    }

    @Override
    public void map(Bundle bundle, IResource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {

        Procedure procedure = (Procedure) resource;

        if (isAlreadyProcessed(procedure, processedList))
            return;

        Obs proceduresObs = new Obs();
        proceduresObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_PROCEDURES_TEMPLATE));

        proceduresObs.addGroupMember(getStartDate(procedure));
        proceduresObs.addGroupMember(getEndDate(procedure));
        //TODO : change Outcome and follow up to look up from codeable concept
//        proceduresObs.addGroupMember(getOutCome(procedure));
//        proceduresObs.addGroupMember(getFollowUp(procedure));
        proceduresObs.addGroupMember(getProcedureType(procedure));

        IResource diagnosticReportResource = getDiagnosticReportResource(bundle, procedure);
        if (diagnosticReportResource != null && diagnosticReportResource instanceof DiagnosticReport) {
            proceduresObs.addGroupMember(getDiagnosisReport((DiagnosticReport) diagnosticReportResource));
        }
        newEmrEncounter.addObs(proceduresObs);
        processedList.put(procedure.getId().getValue(), Arrays.asList(proceduresObs.getUuid()));
    }

    private Obs getDiagnosisReport(DiagnosticReport diagnosticReport) {
        Obs diagnosisStudyObs = new Obs();
        diagnosisStudyObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_STUDY));

        Obs diagnosticTest = mapObservationForConcept(diagnosticReport.getName(), MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_TEST);

        List<ResourceReferenceDt> diagnosisResult = diagnosticReport.getResult();
        Obs result = null;
        if (CollectionUtils.isNotEmpty(diagnosisResult) && !diagnosisResult.get(0).isEmpty()) {
            result = new Obs();
            result.setConcept(conceptService.getConceptByName(MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_RESULT));
            String diagnosticResult = diagnosisResult.get(0).getDisplay().getValue();
            result.setValueText(diagnosticResult);
        }
        List<CodeableConceptDt> codedDiagnosis = diagnosticReport.getCodedDiagnosis();
        Obs diagnosisObs = null;
        if (CollectionUtils.isNotEmpty(codedDiagnosis)) {
            diagnosisObs = mapObservationForConcept(codedDiagnosis.get(0), MRS_CONCEPT_PROCEDURE_DIAGNOSIS);
        }
        diagnosisStudyObs.addGroupMember(diagnosticTest);
        diagnosisStudyObs.addGroupMember(result);
        diagnosisStudyObs.addGroupMember(diagnosisObs);
        return diagnosisStudyObs;
    }

    private IResource getDiagnosticReportResource(Bundle bundle, Procedure procedure) {
        List<ResourceReferenceDt> reportList = procedure.getReport();
        return reportList.isEmpty() ? null : FHIRFeedHelper.findResourceByReference(bundle, reportList.get(0));
    }

    private Obs getProcedureType(Procedure procedure) {
        CodeableConceptDt procedureType = procedure.getType();
        return mapObservationForConcept(procedureType, MRS_CONCEPT_PROCEDURE_TYPE);
    }

    private boolean isAlreadyProcessed(Procedure procedure, Map<String, List<String>> processedList) {
        return processedList.containsKey(procedure.getIdentifier().get(0).getValue());
    }

    private Obs getStartDate(Procedure procedure) {
        PeriodDt period = (PeriodDt) procedure.getPerformed();
        Obs startDate = null;
        if (period != null) {
            startDate = new Obs();
            startDate.setConcept(conceptService.getConceptByName(MRS_CONCEPT_PROCEDURE_START_DATE));
            startDate.setValueDate(period.getStart());
        }
        return startDate;
    }

    private Obs getEndDate(Procedure procedure) {
        PeriodDt period = (PeriodDt) procedure.getPerformed();
        Obs endDate = null;
        if (period != null) {
            endDate = new Obs();
            endDate.setConcept(conceptService.getConceptByName(MRS_CONCEPT_PROCEDURE_END_DATE));
            endDate.setValueDate(period.getEnd());
        }
        return endDate;
    }

    //DO NOT REMOVE these methods
//    private Obs getOutCome(Procedure procedure) {
//        return fhirResourceValueMapper.mapObservationForConcept(procedure.getOutcome(), MRS_CONCEPT_PROCEDURE_OUTCOME);
//    }
//
//    private Obs getFollowUp(Procedure procedure) {
//        return fhirResourceValueMapper.mapObservationForConcept(procedure.getFollowUp(), MRS_CONCEPT_PROCEDURE_FOLLOW_UP);
//    }

    private Obs mapObservationForConcept(CodeableConceptDt codeableConcept, String conceptName) {
        Obs obs = new Obs();
        obs.setConcept(conceptService.getConceptByName(conceptName));
        obs.setValueCoded(omrsConceptLookup.findConceptByCodeOrDisplay(codeableConcept.getCoding()));
        return obs;
    }
}
