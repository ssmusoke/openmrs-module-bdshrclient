package org.openmrs.module.fhir.mapper.bundler;


import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Procedure;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.ObservationType;
import org.openmrs.module.fhir.utils.CodableConceptService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.openmrs.module.fhir.mapper.MRSProperties.*;

@Component
public class ProcedureMapper implements EmrObsResourceHandler {

    private static String DIAGNOSTIC_REPORT_RESOURCE_NAME = "Diagnostic Report";

    @Autowired
    private CodableConceptService codeableConceptService;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    private DiagnosticReportBuilder diagnosticReportBuilder;

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        return obs.isOfType(ObservationType.PROCEDURES);
    }

    @Override
    public List<FHIRResource> map(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties) {
        CompoundObservation compoundObservationProcedure = new CompoundObservation(obs);
        return mapProcedure(obs, fhirEncounter, systemProperties, compoundObservationProcedure);
    }

    private List<FHIRResource> mapProcedure(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties, CompoundObservation compoundObservationProcedure) {
        List<FHIRResource> resources = new ArrayList<>();
        Procedure procedure = new Procedure();

        procedure.setPatient(fhirEncounter.getPatient());
        procedure.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId().getValue()));
        CodeableConceptDt procedureType = getProcedure(compoundObservationProcedure);
        if (procedureType != null) {
            procedure.setType(procedureType);
            setIdentifier(obs, systemProperties, procedure);
            //TODO : how do we set follow up and outcome
//            procedure.setOutcome(getProcedureOutcomeText(compoundObservationProcedure));
//            procedure.setFollowUp(getProcedureFollowUp(compoundObservationProcedure));
            procedure.setPerformed(getProcedurePeriod(compoundObservationProcedure));
            FHIRResource procedureReportResource = addReportToProcedure(compoundObservationProcedure, fhirEncounter, systemProperties, procedure);
            if (procedureReportResource != null) {
                resources.add(procedureReportResource);
            }

            FHIRResource procedureResource = new FHIRResource(MRS_CONCEPT_PROCEDURES_TEMPLATE, procedure.getIdentifier(), procedure);
            resources.add(procedureResource);
        }

        return resources;
    }

    private FHIRResource addReportToProcedure(CompoundObservation compoundObservationProcedure, Encounter fhirEncounter, SystemProperties systemProperties, Procedure procedure) {
        DiagnosticReport diagnosticReport = buildDiagnosticReport(compoundObservationProcedure, fhirEncounter, systemProperties);
        FHIRResource diagnosticReportResource = null;
        if (diagnosticReport != null) {
            diagnosticReportResource = new FHIRResource(DIAGNOSTIC_REPORT_RESOURCE_NAME, diagnosticReport.getIdentifier(), diagnosticReport);
            ResourceReferenceDt diagnosticResourceRef = procedure.addReport();
            diagnosticResourceRef.setReference(diagnosticReportResource.getIdentifier().getValue());
            diagnosticResourceRef.setDisplay(diagnosticReportResource.getResourceName());
        }
        return diagnosticReportResource;
    }


    private CodeableConceptDt getProcedure(CompoundObservation compoundObservationProcedure) {
        CodeableConceptDt procedureType = null;
        Obs procedureTypeObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_TYPE);
        if (procedureTypeObs != null) {
            Concept valueCoded = procedureTypeObs.getValueCoded();
            procedureType = codeableConceptService.addTRCodingOrDisplay(valueCoded, idMappingsRepository);
        }
        return procedureType!= null && !procedureType.isEmpty() ? procedureType : null;
    }

    private void setIdentifier(Obs obs, SystemProperties systemProperties, Procedure procedure) {
        String id = new EntityReference().build(Obs.class, systemProperties, obs.getUuid());
        procedure.addIdentifier().setValue(id);
        procedure.setId(id);
    }

    //DO NOT REMOVE this unused method
    private String getProcedureOutcomeText(CompoundObservation compoundObservationProcedure) {
        Obs outcomeObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_OUTCOME);
        if (outcomeObs != null) {
            return outcomeObs.getValueText();
        }
        return null;
    }

    //DO NOT REMOVE this unused method
    private String getProcedureFollowUp(CompoundObservation compoundObservationProcedure) {
        Obs followupObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_FOLLOW_UP);
        if (followupObs != null) {
            return followupObs.getValueText();
        }
        return null;
    }


    private PeriodDt getProcedurePeriod(CompoundObservation compoundObservationProcedure) {
        Obs startDateObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_START_DATE);
        Obs endDateObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_END_DATE);
        return getPeriod(startDateObs, endDateObs);
    }

    private PeriodDt getPeriod(Obs startDateObs, Obs endDateObs) {
        PeriodDt period = new PeriodDt();
        if (startDateObs != null) period.setStart(startDateObs.getValueDate(), TemporalPrecisionEnum.MILLI);
        if (startDateObs != null && endDateObs != null) period.setEnd(endDateObs.getValueDate(), TemporalPrecisionEnum.MILLI);
        return period;
    }

    private DiagnosticReport buildDiagnosticReport(CompoundObservation compoundObservationProcedure, Encounter fhirEncounter, SystemProperties systemProperties) {
        DiagnosticReport diagnosticReport = null;
        Obs diagnosticStudyObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_STUDY);
        CompoundObservation compoundDiagnosticStudyObs = new CompoundObservation(diagnosticStudyObs);
        CodeableConceptDt diagnosisTestName = getNameToDiagnosticReport(compoundDiagnosticStudyObs);
        if (diagnosisTestName != null) {
            diagnosticReport = diagnosticReportBuilder.build(diagnosticStudyObs, fhirEncounter, systemProperties);
            diagnosticReport.setName(diagnosisTestName);
            setResultToDiagnosticReport(diagnosticReport, compoundDiagnosticStudyObs);
            setDiagnosisToDiagnosticReport(diagnosticReport, compoundDiagnosticStudyObs);
        }
        return diagnosticReport;
    }

    private void setDiagnosisToDiagnosticReport(DiagnosticReport diagnosticReport, CompoundObservation compoundDiagnosticStudyObs) {
        Obs diagnosisObs = compoundDiagnosticStudyObs.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_DIAGNOSIS);
        CodeableConceptDt codeableType = diagnosisObs != null ? codeableConceptService.addTRCodingOrDisplay(diagnosisObs.getValueCoded(), idMappingsRepository) : null;
        if (codeableType != null && !codeableType.isEmpty()) {
            CodeableConceptDt codedDiagnosis = diagnosticReport.addCodedDiagnosis();
            codedDiagnosis.getCoding().addAll(codeableType.getCoding());
        }
    }

    private void setResultToDiagnosticReport(DiagnosticReport diagnosticReport, CompoundObservation compoundDiagnosticStudyObs) {
        Obs diagnosticResultObs = compoundDiagnosticStudyObs.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_RESULT);
        String result = diagnosticResultObs != null ? diagnosticResultObs.getValueText() : null;
        if (result != null) {
            ResourceReferenceDt resultReference = diagnosticReport.addResult();
            resultReference.setDisplay(result);
        }
    }

    private CodeableConceptDt getNameToDiagnosticReport(CompoundObservation compoundDiagnosticStudyObs) {
        CodeableConceptDt name = null;
        if (compoundDiagnosticStudyObs.getRawObservation() != null) {
            Obs diagnosticTestObs = compoundDiagnosticStudyObs.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_TEST);
            name = diagnosticTestObs != null ? codeableConceptService.addTRCodingOrDisplay(diagnosticTestObs.getValueCoded(), idMappingsRepository) : null;
        }
        return name != null && name.isEmpty() ? null : name;
    }
}
