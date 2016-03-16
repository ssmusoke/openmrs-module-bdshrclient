package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticReportStatusEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;

import java.util.Date;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.junit.Assert.assertEquals;

public class DiagnosticReportBuilderTest {

    @Test
    public void shouldMapObservationToDiagnosticReport() throws Exception {
        Obs obs = new Obs(1);
        Date date = new Date();
        obs.setDateCreated(date);
        obs.setObsDatetime(date);

        String encounterId = "shr-enc-id";
        FHIREncounter fhirEncounter = buildEncounter(encounterId, "HID", "http://pr.com/provider/123.json");
        DiagnosticReport report = new DiagnosticReportBuilder().build(obs, fhirEncounter, MapperTestHelper.getSystemProperties("1"));
        assertEquals(encounterId, report.getEncounter().getReference().getIdPart());
        assertEquals(DiagnosticReportStatusEnum.FINAL.getCode(), report.getStatus());
        assertEquals(date, report.getIssued());
        assertEquals(date, ((DateTimeDt) report.getEffective()).getValue());
        assertEquals(fhirEncounter.getPatient(), report.getSubject());
        String reportId = "urn:uuid:" + obs.getUuid();
        assertEquals(reportId, report.getId().getValue());
        assertEquals(1, report.getIdentifier().size());
        assertEquals(reportId, report.getIdentifierFirstRep().getValue());
    }

    private FHIREncounter buildEncounter(String encounterId, String hid, String providerUrl) {
        Encounter fhirEncounter = new Encounter();
        fhirEncounter.setId(encounterId);
        fhirEncounter.setIdentifier(asList(encounterId));
        fhirEncounter.setPatient(new ResourceReferenceDt().setReference(hid));
        Encounter.Participant participant = fhirEncounter.addParticipant();
        participant.setIndividual(new ResourceReferenceDt().setReference(providerUrl));
        return new FHIREncounter(fhirEncounter);
    }
}