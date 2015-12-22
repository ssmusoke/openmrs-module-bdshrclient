package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticReportStatusEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.stereotype.Component;

@Component("diagnosticReportBuilder")
public class DiagnosticReportBuilder {
    public DiagnosticReport build(Obs obs, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        DiagnosticReport report = new DiagnosticReport();
        report.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId()));
        report.setStatus(DiagnosticReportStatusEnum.FINAL);
        report.setIssued(obs.getObsDatetime(), TemporalPrecisionEnum.MILLI);
        report.setSubject(fhirEncounter.getPatient());
        String reportId = new EntityReference().build(Obs.class, systemProperties, obs.getUuid());
        report.addIdentifier(new IdentifierDt().setValue(reportId));
        report.setId(reportId);
        report.setPerformer(fhirEncounter.getFirstParticipantReference());

        DateTimeDt diagnostic = new DateTimeDt();
        diagnostic.setValue(obs.getDateCreated(), TemporalPrecisionEnum.MILLI);
        report.setEffective(diagnostic);
        return report;
    }
}
