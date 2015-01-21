package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.*;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.CodableConceptService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.hl7.fhir.instance.model.DiagnosticReport.DiagnosticReportStatus;

@Component("diagnosticReportBuilder")
public class DiagnosticReportBuilder {

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    private CodableConceptService codableConceptService;


    public DiagnosticReport build(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties) {
        DiagnosticReport report = new DiagnosticReport();
        report.setStatus(new Enumeration<>(DiagnosticReportStatus.final_));
        report.setIssuedSimple(new DateAndTime(obs.getObsDatetime()));
        report.setSubject(fhirEncounter.getSubject());
        Identifier identifier = new Identifier();
        identifier.setValueSimple(new EntityReference().build(Obs.class, systemProperties, obs.getUuid()));
        report.setIdentifier(identifier);
        report.setPerformer(getParticipant(fhirEncounter));

        DateTime diagnostic = new DateTime();
        diagnostic.setValue(new DateAndTime(obs.getDateCreated()));
        report.setDiagnostic(diagnostic);


        return report;
    }

    //TODO : how do we identify this individual?
    protected ResourceReference getParticipant(Encounter encounter) {
        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
        if ((participants != null) && !participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }
}
