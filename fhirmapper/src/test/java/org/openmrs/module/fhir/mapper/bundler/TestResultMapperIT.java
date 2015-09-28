package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.*;
import org.junit.After;
import org.junit.Test;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.text.SimpleDateFormat;
import java.util.List;

import static org.hl7.fhir.instance.model.DiagnosticReport.DiagnosticReportStatus.final_;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;
import static org.openmrs.module.fhir.TestFhirFeedHelper.getResourceByReference;
import static org.openmrs.module.fhir.TestFhirFeedHelper.getResourceByType;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TestResultMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private TestResultMapper testResultMapper;
    @Autowired
    private ObsService obsService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ConceptService conceptService;

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldMapLabTestResults() throws Exception {
        executeDataSet("testDataSets/labResultDS.xml");
        Encounter fhirEncounter = buildEncounter();
        ResourceReference patientHid = new ResourceReference();
        patientHid.setReferenceSimple("patientHid");
        fhirEncounter.setSubject(patientHid);
        List<FHIRResource> FHIRResources = testResultMapper.map(obsService.getObs(1), fhirEncounter, getSystemProperties("1"));
        assertNotNull(FHIRResources);
        assertEquals(2, FHIRResources.size());
        FHIRResource diagnosticReportResource = getResourceByType(ResourceType.DiagnosticReport, FHIRResources);
        DiagnosticReport report = (DiagnosticReport) diagnosticReportResource.getResource();
        assertEquals(fhirEncounter.getSubject(), report.getSubject());
        assertEquals(fhirEncounter.getParticipant().get(0).getIndividual(), report.getPerformer());
        assertDiagnosticReport(report, FHIRResources);
        assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(obsService.getObs(2).getObsDatetime()), report.getIssuedSimple().toString());
        assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(orderService.getOrder(17).getDateActivated()), ((DateTime) report.getDiagnostic()).getValue().toString());
        assertEquals(conceptService.getConcept(107).getName().getName(), report.getName().getCoding().get(0).getDisplaySimple());
    }

    @Test
    public void shouldMapLabPanelResults() throws Exception {
        executeDataSet("testDataSets/labResultDS.xml");
        Encounter fhirEncounter = buildEncounter();
        ResourceReference patientHid = new ResourceReference();
        patientHid.setReferenceSimple("patientHid");
        fhirEncounter.setSubject(patientHid);
        List<FHIRResource> FHIRResources = testResultMapper.map(obsService.getObs(11), fhirEncounter, getSystemProperties("1"));
        assertNotNull(FHIRResources);
        assertEquals(6, FHIRResources.size());
        for (FHIRResource FHIRResource : FHIRResources) {
            if(FHIRResource.getResource() instanceof DiagnosticReport) {
                DiagnosticReport report = (DiagnosticReport) FHIRResource.getResource();
                assertEquals(fhirEncounter.getSubject(), report.getSubject());
                assertEquals(fhirEncounter.getParticipant().get(0).getIndividual(), report.getPerformer());
                assertDiagnosticReport(report, FHIRResources);
            }
        }
    }

    private void assertDiagnosticReport(DiagnosticReport report, List<FHIRResource> FHIRResources) {
        FHIRResource observationResource = getResourceByReference(report.getResult().get(0), FHIRResources);
        assertNotNull(observationResource);
        assertFalse(report.getResult().isEmpty());
        assertNotNull(report.getIdentifier());

        assertEquals(final_, report.getStatus().getValue());
        assertFalse(report.getRequestDetail().isEmpty());
        assertTrue(report.getRequestDetail().get(0).getReferenceSimple().startsWith("http://172.18.46.57:8081/patients/hid/encounters/shrEncounterId"));
        assertEquals(observationResource.getIdentifier().getValueSimple(), report.getResult().get(0).getReferenceSimple());
        Observation observation = (Observation) observationResource.getResource();
        assertNotNull(observation.getValue());
        if(observation.getValue() instanceof Decimal) {
            assertEquals("120.0", ((Decimal) observation.getValue()).getStringValue());
        }
        assertEquals(obsService.getObs(4).getValueText(), report.getConclusionSimple());
    }

    private Encounter buildEncounter() {
        Encounter fhirEncounter = new Encounter();
        ResourceReference subject = new ResourceReference();
        subject.setReferenceSimple("patient 1");
        fhirEncounter.setSubject(subject);
        Encounter.EncounterParticipantComponent participant = fhirEncounter.addParticipant();
        ResourceReference individual = new ResourceReference();
        individual.setReferenceSimple("Provider 1");
        participant.setIndividual(individual);
        return fhirEncounter;
    }
}