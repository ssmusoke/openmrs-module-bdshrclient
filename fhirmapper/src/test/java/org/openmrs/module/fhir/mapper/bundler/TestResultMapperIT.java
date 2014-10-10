package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.*;
import org.junit.Test;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.List;

import static org.hl7.fhir.instance.model.DiagnosticReport.DiagnosticReportStatus.final_;
import static org.junit.Assert.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class TestResultMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private TestResultMapper testResultMapper;
    @Autowired
    private ObsService obsService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ConceptService conceptService;

    @Test
    public void shouldMapLabTestResults() throws Exception {
        executeDataSet("labResultDS.xml");
        Encounter fhirEncounter = new Encounter();
        ResourceReference subject = new ResourceReference();
        subject.setReferenceSimple("patient 1");
        fhirEncounter.setSubject(subject);
        Encounter.EncounterParticipantComponent participant = fhirEncounter.addParticipant();
        ResourceReference individual = new ResourceReference();
        individual.setReferenceSimple("Provider 1");
        participant.setIndividual(individual);
        List<EmrResource> emrResources = testResultMapper.map(obsService.getObs(1), fhirEncounter);
        assertNotNull(emrResources);
        assertEquals(2, emrResources.size());
        DiagnosticReport report = (DiagnosticReport) emrResources.get(1).getResource();
        assertFalse(report.getResult().isEmpty());

        assertEquals(conceptService.getConcept(107).getName().getName(), report.getName().getCoding().get(0).getDisplaySimple());
        assertEquals(final_, report.getStatus().getValue());
        assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(obsService.getObs(2).getObsDatetime()), report.getIssuedSimple().toString());
        assertEquals(fhirEncounter.getSubject(), report.getSubject());
        assertEquals(fhirEncounter.getParticipant().get(0).getIndividual(), report.getPerformer());
        assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(orderService.getOrder(17).getDateActivated()), ((DateTime)report.getDiagnostic()).getValue().toString());
        //assertEquals(report.getRequestDetail());
        assertEquals(emrResources.get(0).getIdentifier().getValueSimple(), report.getResult().get(0).getReferenceSimple());
        assertEquals("120", ((Decimal)((Observation) emrResources.get(0).getResource()).getValue()).getStringValue());
        assertEquals(obsService.getObs(4).getValueText(), report.getConclusionSimple());
    }
}