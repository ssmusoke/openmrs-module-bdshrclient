package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticReportStatusEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.DecimalDt;
import org.junit.After;
import org.junit.Test;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.TestFhirFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;
import static org.openmrs.module.fhir.TestFhirFeedHelper.getResourceByReference;

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
        ResourceReferenceDt patientHid = new ResourceReferenceDt();
        patientHid.setReference("patientHid");
        fhirEncounter.setPatient(patientHid);
        List<FHIRResource> fhirResources = testResultMapper.map(obsService.getObs(1), fhirEncounter, getSystemProperties("1"));
        assertNotNull(fhirResources);
        assertEquals(2, fhirResources.size());
        FHIRResource diagnosticReportResource = TestFhirFeedHelper.getResourceByType(new DiagnosticReport().getResourceName(), fhirResources);
        DiagnosticReport report = (DiagnosticReport) diagnosticReportResource.getResource();
        assertEquals(fhirEncounter.getPatient(), report.getSubject());
        assertEquals(fhirEncounter.getParticipant().get(0).getIndividual(), report.getPerformer());
        assertDiagnosticReport(report, fhirResources);
        assertEquals(obsService.getObs(2).getObsDatetime(), report.getIssued());
        assertEquals(orderService.getOrder(17).getDateActivated(), ((DateTimeDt) report.getEffective()).getValue());
        assertEquals(conceptService.getConcept(107).getName().getName(), report.getCode().getCoding().get(0).getDisplay());
    }

    @Test
    public void shouldMapLabPanelResults() throws Exception {
        executeDataSet("testDataSets/labResultDS.xml");
        Encounter fhirEncounter = buildEncounter();
        ResourceReferenceDt patientHid = new ResourceReferenceDt();
        patientHid.setReference("patientHid");
        fhirEncounter.setPatient(patientHid);
        List<FHIRResource> FHIRResources = testResultMapper.map(obsService.getObs(11), fhirEncounter, getSystemProperties("1"));
        assertNotNull(FHIRResources);
        assertEquals(6, FHIRResources.size());
        for (FHIRResource FHIRResource : FHIRResources) {
            if (FHIRResource.getResource() instanceof DiagnosticReport) {
                DiagnosticReport report = (DiagnosticReport) FHIRResource.getResource();
                assertEquals(fhirEncounter.getPatient(), report.getSubject());
                assertEquals(fhirEncounter.getParticipant().get(0).getIndividual(), report.getPerformer());
                assertDiagnosticReport(report, FHIRResources);
            }
        }
    }

    private void assertDiagnosticReport(DiagnosticReport report, List<FHIRResource> fhirResources) {
        FHIRResource observationResource = getResourceByReference(report.getResult().get(0), fhirResources);
        assertNotNull(observationResource);
        assertFalse(report.getResult().isEmpty());
        assertNotNull(report.getIdentifier());

        assertEquals(DiagnosticReportStatusEnum.FINAL.getCode(), report.getStatus());
        assertFalse(report.getRequest().isEmpty());
        assertTrue(report.getRequest().get(0).getReference().getValue().startsWith("http://172.18.46.57:8081/patients/hid/encounters/shrEncounterId"));
        assertEquals(observationResource.getIdentifier().getValue(), report.getResult().get(0).getReference().getValue());
        Observation observation = (Observation) observationResource.getResource();
        assertNotNull(observation.getValue());
        if (observation.getValue() instanceof DecimalDt) {
            assertTrue(120.0 == ((DecimalDt) observation.getValue()).getValue().doubleValue());
        }
        assertEquals(obsService.getObs(4).getValueText(), report.getConclusion());
    }

    private Encounter buildEncounter() {
        Encounter fhirEncounter = new Encounter();
        ResourceReferenceDt subject = new ResourceReferenceDt();
        subject.setReference("patient 1");
        fhirEncounter.setPatient(subject);
        Encounter.Participant participant = fhirEncounter.addParticipant();
        ResourceReferenceDt individual = new ResourceReferenceDt();
        individual.setReference("Provider 1");
        participant.setIndividual(individual);
        return fhirEncounter;
    }
}