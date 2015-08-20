package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticReportStatusEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("diagnosticReportBuilder")
public class DiagnosticReportBuilder {
    public DiagnosticReport build(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties) {
        DiagnosticReport report = new DiagnosticReport();
        report.setEncounter(new ResourceReferenceDt().setReference(new EntityReference().build(Encounter.class, systemProperties, fhirEncounter.getId().getValueAsString())));
        report.setStatus(DiagnosticReportStatusEnum.FINAL);
        report.setIssued(obs.getObsDatetime(), TemporalPrecisionEnum.MILLI);
        report.setSubject(fhirEncounter.getPatient());
        String reportId = new EntityReference().build(Obs.class, systemProperties, obs.getUuid());
        report.addIdentifier(new IdentifierDt().setValue(reportId));
        report.setId(reportId);
        report.setPerformer(getParticipant(fhirEncounter));

        DateTimeDt diagnostic = new DateTimeDt();
        diagnostic.setValue(obs.getDateCreated(), TemporalPrecisionEnum.MILLI);
        report.setDiagnostic(diagnostic);
        return report;
    }

    protected ResourceReferenceDt getParticipant(Encounter encounter) {
        List<Encounter.Participant> participants = encounter.getParticipant();
        if ((participants != null) && !participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }
}
