package org.openmrs.module.fhir.mapper.emr;


import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.resource.Procedure;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.TrValueSetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.openmrs.module.fhir.mapper.MRSProperties.*;

@Component
public class FHIRProcedureMapper implements FHIRResourceMapper {

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    @Autowired
    private FHIRObservationValueMapper observationValueMapper;

    @Override
    public boolean canHandle(IResource resource) {
        return resource instanceof Procedure;
    }

    @Override
    public void map(Bundle bundle, IResource resource, Patient emrPatient, Encounter newEmrEncounter) {
        Procedure procedure = (Procedure) resource;

        Obs proceduresObs = new Obs();
        proceduresObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_PROCEDURES_TEMPLATE));

        proceduresObs.addGroupMember(getStartDate(procedure));
        proceduresObs.addGroupMember(getEndDate(procedure));
        proceduresObs.addGroupMember(getOutCome(procedure));
        setFollowUpObses(procedure, proceduresObs);
        proceduresObs.addGroupMember(getProcedureType(procedure));
        proceduresObs.addGroupMember(getProcedureNotesObs(procedure));
        proceduresObs.addGroupMember(getProcedureStatusObs(procedure));

        for (ResourceReferenceDt reportReference : procedure.getReport()) {
            IResource diagnosticReportResource = FHIRFeedHelper.findResourceByReference(bundle, reportReference);
            if (diagnosticReportResource != null && diagnosticReportResource instanceof DiagnosticReport) {
                proceduresObs.addGroupMember(getDiagnosisStudyObs((DiagnosticReport) diagnosticReportResource, bundle));
            }
        }
        newEmrEncounter.addObs(proceduresObs);
    }

    private Obs getProcedureStatusObs(Procedure procedure) {
        Obs statusObs = new Obs();
        statusObs.setConcept(omrsConceptLookup.findTRConceptOfType(TrValueSetType.PROCEDURE_STATUS));
        Concept statusConcept = omrsConceptLookup.findValuesetConceptFromTrValuesetType(TrValueSetType.PROCEDURE_STATUS, procedure.getStatus());
        statusObs.setValueCoded(statusConcept);
        return statusObs;
    }

    private Obs getProcedureNotesObs(Procedure procedure) {
        if (procedure.getNotes() != null && !procedure.getNotes().isEmpty()) {
            Obs procedureNotesObs = new Obs();
            procedureNotesObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_PROCEDURE_NOTES));
            procedureNotesObs.setValueText(procedure.getNotes());
            return procedureNotesObs;
        }
        return null;
    }

    private Obs getDiagnosisStudyObs(DiagnosticReport diagnosticReport, Bundle bundle) {
        Obs diagnosisStudyObs = new Obs();
        diagnosisStudyObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_STUDY));

        Obs diagnosticTest = mapObservationForConcept(diagnosticReport.getName(), MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_TEST);
        diagnosisStudyObs.addGroupMember(diagnosticTest);

        addDiagnosticResults(diagnosticReport, bundle, diagnosisStudyObs);
        addCodedDiagnoses(diagnosticReport, diagnosisStudyObs);
        return diagnosisStudyObs;
    }

    private void addCodedDiagnoses(DiagnosticReport diagnosticReport, Obs diagnosisStudyObs) {
        for (CodeableConceptDt diagnosis : diagnosticReport.getCodedDiagnosis()) {
            Obs diagnosisObs = mapObservationForConcept(diagnosis, MRS_CONCEPT_PROCEDURE_DIAGNOSIS);
            diagnosisStudyObs.addGroupMember(diagnosisObs);
        }
    }

    private void addDiagnosticResults(DiagnosticReport diagnosticReport, Bundle bundle, Obs diagnosisStudyObs) {
        Concept diagnosticResultConcept = conceptService.getConceptByName(MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_RESULT);
        for (ResourceReferenceDt resultReference : diagnosticReport.getResult()) {
            Obs result = new Obs();
            result.setConcept(diagnosticResultConcept);
            Observation resultObservation = (Observation) FHIRFeedHelper.findResourceByReference(bundle, resultReference);
            observationValueMapper.map(resultObservation.getValue(), result);
            diagnosisStudyObs.addGroupMember(result);
        }
    }

    private Obs getProcedureType(Procedure procedure) {
        CodeableConceptDt procedureType = procedure.getType();
        return mapObservationForConcept(procedureType, MRS_CONCEPT_PROCEDURE_TYPE);
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

    private Obs getOutCome(Procedure procedure) {
        if (procedure.getOutcome() != null && !procedure.getOutcome().isEmpty()) {
            Obs outcome = new Obs();
            outcome.setConcept(omrsConceptLookup.findTRConceptOfType(TrValueSetType.PROCEDURE_OUTCOME));
            outcome.setValueCoded(omrsConceptLookup.findConceptByCodeOrDisplay(procedure.getOutcome().getCoding()));
            return outcome;
        }
        return null;
    }

    private void setFollowUpObses(Procedure procedure, Obs procedureObs) {
        for (CodeableConceptDt followUp : procedure.getFollowUp()) {
            Obs followUpObs = new Obs();
            followUpObs.setConcept(omrsConceptLookup.findTRConceptOfType(TrValueSetType.PROCEDURE_FOLLOWUP));
            followUpObs.setValueCoded(omrsConceptLookup.findConceptByCodeOrDisplay(followUp.getCoding()));
            procedureObs.addGroupMember(followUpObs);
        }
    }

    private Obs mapObservationForConcept(CodeableConceptDt codeableConcept, String conceptName) {
        Obs obs = new Obs();
        obs.setConcept(conceptService.getConceptByName(conceptName));
        obs.setValueCoded(omrsConceptLookup.findConceptByCodeOrDisplay(codeableConcept.getCoding()));
        return obs;
    }
}
