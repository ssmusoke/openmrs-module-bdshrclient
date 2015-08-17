package org.openmrs.module.fhir.mapper.emr;


import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
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
    FHIRResourceValueMapper fhirResourceValueMapper;

    @Autowired
    FHIRDiagnosticReportMapper fhirDiagnosticReportMapper;

    @Override
    public boolean canHandle(Resource resource) {
        return ResourceType.Procedure.equals(resource.getResourceType());
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {

        Procedure procedure = (Procedure) resource;

        if (isAlreadyProcessed(procedure, processedList))
            return;

        Obs proceduresObs = new Obs();
        proceduresObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_PROCEDURES_TEMPLATE));

        proceduresObs.addGroupMember(getStartDate(procedure));
        proceduresObs.addGroupMember(getEndDate(procedure));
        proceduresObs.addGroupMember(getOutCome(procedure));
        proceduresObs.addGroupMember(getFollowUp(procedure));
        proceduresObs.addGroupMember(getProcedureType(procedure));

        Resource diagnosticReportResource = getDiagnosticReportResource(feed, procedure);
        if (diagnosticReportResource != null && diagnosticReportResource instanceof DiagnosticReport) {
            proceduresObs.addGroupMember(getDiagnosisReport((DiagnosticReport) diagnosticReportResource));
        }
        newEmrEncounter.addObs(proceduresObs);
        processedList.put(procedure.getIdentifier().get(0).getValueSimple(), Arrays.asList(proceduresObs.getUuid()));
    }

    private Obs getDiagnosisReport(DiagnosticReport diagnosticReport) {
        Obs diagnosisStudyObs = new Obs();
        diagnosisStudyObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_STUDY));

        Obs diagnosticTest = fhirResourceValueMapper.mapObservationForConcept(diagnosticReport.getName(), MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_TEST);

        List<ResourceReference> diagnosisResult = diagnosticReport.getResult();
        Obs result = null;
        if (CollectionUtils.isNotEmpty(diagnosisResult)) {
            result = fhirResourceValueMapper.mapObservationForConcept(diagnosisResult.get(0).getDisplay(), MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_RESULT);
        }
        List<CodeableConcept> codedDiagnosis = diagnosticReport.getCodedDiagnosis();
        Obs diagnosisObs = null;
        if (CollectionUtils.isNotEmpty(codedDiagnosis)) {
            diagnosisObs = fhirResourceValueMapper.mapObservationForConcept(codedDiagnosis.get(0), MRS_CONCEPT_PROCEDURE_DIAGNOSIS);
        }
        diagnosisStudyObs.addGroupMember(diagnosticTest);
        diagnosisStudyObs.addGroupMember(result);
        diagnosisStudyObs.addGroupMember(diagnosisObs);
        return diagnosisStudyObs;
    }

    private Resource getDiagnosticReportResource(AtomFeed feed, Procedure procedure) {
        List<ResourceReference> reportList = procedure.getReport();
        return reportList.isEmpty() ? null : FHIRFeedHelper.findResourceByReference(feed, reportList.get(0));
    }

    private Obs getProcedureType(Procedure procedure) {

        CodeableConcept procedureType = procedure.getType();
        return fhirResourceValueMapper.mapObservationForConcept(procedureType, MRS_CONCEPT_PROCEDURE_TYPE);
    }

    private boolean isAlreadyProcessed(Procedure procedure, Map<String, List<String>> processedList) {
        return processedList.containsKey(procedure.getIdentifier().get(0).getValueSimple());
    }

    private Obs getStartDate(Procedure procedure) {
        Period period = procedure.getDate();
        Obs startDate = null;
        if (period != null) {
            startDate = fhirResourceValueMapper.mapObservationForConcept(period.getStart(), MRS_CONCEPT_PROCEDURE_START_DATE);
        }
        return startDate;
    }

    private Obs getEndDate(Procedure procedure) {
        Period period = procedure.getDate();
        Obs endDate = null;
        if (period != null) {
            endDate = fhirResourceValueMapper.mapObservationForConcept(period.getEnd(), MRS_CONCEPT_PROCEDURE_END_DATE);
        }
        return endDate;
    }

    private Obs getOutCome(Procedure procedure) {
        return fhirResourceValueMapper.mapObservationForConcept(procedure.getOutcome(), MRS_CONCEPT_PROCEDURE_OUTCOME);
    }

    private Obs getFollowUp(Procedure procedure) {
        return fhirResourceValueMapper.mapObservationForConcept(procedure.getFollowUp(), MRS_CONCEPT_PROCEDURE_FOLLOW_UP);
    }
}
