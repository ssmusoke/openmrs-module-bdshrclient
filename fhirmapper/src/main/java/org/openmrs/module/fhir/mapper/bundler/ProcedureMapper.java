package org.openmrs.module.fhir.mapper.bundler;


import org.hl7.fhir.instance.model.*;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.bundler.condition.ObservationValueMapper;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.ObservationType;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.openmrs.module.fhir.mapper.MRSProperties.*;

@Component
public class ProcedureMapper implements EmrObsResourceHandler {

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private ObservationValueMapper obsValueMapper;

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

        procedure.setSubject(fhirEncounter.getSubject());
        CodeableConcept procedureType = getProcedure(compoundObservationProcedure);
        if (procedureType != null) {
            procedure.setType(procedureType);
            setIdentifier(obs, systemProperties, procedure);
            procedure.setOutcomeSimple(getProcedureOutcomeText(compoundObservationProcedure));
            procedure.setFollowUpSimple(getProcedureFollowUp(compoundObservationProcedure));
            procedure.setDate(getProcedurePeriod(compoundObservationProcedure));

            FHIRResource procedureReportResource = addReportToProcedure(compoundObservationProcedure, fhirEncounter, systemProperties, procedure);
            if (procedureReportResource != null) {
                resources.add(procedureReportResource);
            }

            FHIRResource procedureResource = new FHIRResource(MRS_CONCEPT_PROCEDURES, procedure.getIdentifier(), procedure);
            resources.add(procedureResource);
        }

        return resources;
    }

    private FHIRResource addReportToProcedure(CompoundObservation compoundObservationProcedure, Encounter fhirEncounter, SystemProperties systemProperties, Procedure procedure) {
        DiagnosticReport diagnosticReport = buildDiagnosticReport(compoundObservationProcedure, fhirEncounter, systemProperties);
        FHIRResource diagnosticReportResource = null;
        if (diagnosticReport != null) {
            diagnosticReportResource = new FHIRResource(MRS_DIAGNOSIS_REPORT_RESOURCE_NAME, Arrays.asList(diagnosticReport.getIdentifier()), diagnosticReport);
            ResourceReference diagnosticResourceRef = procedure.addReport();
            diagnosticResourceRef.setReferenceSimple(diagnosticReportResource.getIdentifier().getValueSimple());
            diagnosticResourceRef.setDisplaySimple(diagnosticReportResource.getResourceName());
        }

        return diagnosticReportResource;

    }


    private CodeableConcept getProcedure(CompoundObservation compoundObservationProcedure) {
        CodeableConcept procedureType = null;
        Obs procedureTypeObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_TYPE);
        if (procedureTypeObs != null) {
            Type codeableType = obsValueMapper.map(procedureTypeObs);
            if (codeableType != null && codeableType instanceof CodeableConcept) {
                procedureType = (CodeableConcept) codeableType;
            }
        }
        return procedureType;

    }

    private void setIdentifier(Obs obs, SystemProperties systemProperties, Procedure procedure) {
        Identifier identifier = procedure.addIdentifier();
        identifier.setValueSimple(new EntityReference().build(Obs.class, systemProperties, obs.getUuid()));
    }

    private String getProcedureOutcomeText(CompoundObservation compoundObservationProcedure) {

        Obs outcomeObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_OUTCOME);
        if (outcomeObs != null) {
            return ((String_) obsValueMapper.map(outcomeObs)).getValue();
        }
        return null;
    }

    private String getProcedureFollowUp(CompoundObservation compoundObservationProcedure) {
        Obs followupObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_FOLLOW_UP);
        if (followupObs != null) {
            return ((String_) obsValueMapper.map(followupObs)).getValue();
        }
        return null;
    }


    private Period getProcedurePeriod(CompoundObservation compoundObservationProcedure) {
        Obs startDateObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_START_DATE);
        Obs endDateObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_END_DATE);

        DateTime startDate= getDate(startDateObs);
        DateTime endDate= getDate(endDateObs);

        return getPeriod(startDate, endDate);

    }

    private Period getPeriod(DateTime startDate, DateTime endDate) {
        if(startDate== null && endDate== null){
            return null;
        }
        Period period= new Period();
        period.setStart(startDate);
        period.setEnd(endDate);

        return period;
    }

    private DateTime getDate(Obs dateObs) {
        if (dateObs != null) {
            DateTime date= new DateTime();
            date.setValue(((Date) obsValueMapper.map(dateObs)).getValue());
            return date;
        }
        return  null;
    }

    private DiagnosticReport buildDiagnosticReport(CompoundObservation compoundObservationProcedure, Encounter fhirEncounter, SystemProperties systemProperties) {

        DiagnosticReport diagnosticReport = null;
        Obs diagnosticStudyObs = compoundObservationProcedure.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_STUDY);
        CompoundObservation compoundDiagnosticStudyObs = new CompoundObservation(diagnosticStudyObs);
        CodeableConcept diagnosisTestName = getNameToDiagnosticReport(compoundDiagnosticStudyObs);
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
        Type codeableType = diagnosisObs != null ?obsValueMapper.map(diagnosisObs) : null;
        if (codeableType != null && codeableType instanceof CodeableConcept) {
            CodeableConcept codedDiagnosis = diagnosticReport.addCodedDiagnosis();
            CodeableConcept codeableConcept = (CodeableConcept) codeableType;
            codedDiagnosis.getCoding().addAll(codeableConcept.getCoding());
        }

    }

    private void setResultToDiagnosticReport(DiagnosticReport diagnosticReport, CompoundObservation compoundDiagnosticStudyObs) {
        Obs diagnosticResultObs = compoundDiagnosticStudyObs.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_RESULT);
        Type result = diagnosticResultObs != null ?obsValueMapper.map(diagnosticResultObs) : null;
        if (result != null && result instanceof String_) {
            ResourceReference resultReference = diagnosticReport.addResult();
            resultReference.setDisplaySimple(((String_) result).getValue());
        }

    }

    private CodeableConcept getNameToDiagnosticReport(CompoundObservation compoundDiagnosticStudyObs) {
        if (compoundDiagnosticStudyObs.getRawObservation() != null) {
            Obs diagnosticTestObs = compoundDiagnosticStudyObs.getMemberObsForConceptName(MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_TEST);
            Type name = diagnosticTestObs != null ? obsValueMapper.map(diagnosticTestObs) : null;
            if (name != null && name instanceof CodeableConcept) {
                return (CodeableConcept) name;
            }
        }

        return null;
    }


}
