package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticReportStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir.FHIRProperties;
import org.openmrs.module.fhir.TestFhirFeedHelper;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.module.fhir.MapperTestHelper.containsCoding;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;
import static org.openmrs.module.fhir.TestFhirFeedHelper.getResourceByReference;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TestResultMapperIT extends BaseModuleWebContextSensitiveTest {

    private final String encounterId;
    private final String patientHid;
    private final String providerId;
    @Autowired
    private TestResultMapper testResultMapper;
    @Autowired
    private ObsService obsService;

    public TestResultMapperIT() {
        encounterId = "enc-1";
        patientHid = "patientHid";
        providerId = "Provider 1";
    }

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/labResultDS.xml");
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldMapLabTestResults() throws Exception {
        Encounter fhirEncounter = buildEncounter();

        List<FHIRResource> fhirResources = testResultMapper.map(obsService.getObs(1), new FHIREncounter(fhirEncounter), getSystemProperties("1"));

        assertNotNull(fhirResources);
        assertEquals(2, fhirResources.size());
        FHIRResource diagnosticReportResource = TestFhirFeedHelper.getFirstResourceByType(new DiagnosticReport().getResourceName(), fhirResources);

        assertDiagnosticReportWithResult(fhirResources, diagnosticReportResource, "Urea Nitorgen",
                "http://localhost:9997/openmrs/ws/rest/v1/tr/concept/501qb827-a67c-4q1f-a705-e5efe0qjki2w",
                "501qb827-a67c-4q1f-a705-e5efe0qjki2w",
                "http://localhost:9997/patients/hid/encounters/shrEncounterId1",
                new QuantityDt(120.0), "Lab Notes");
    }

    @Test
    public void shouldMapLabTestResultsWithoutValue() throws Exception {
        Encounter fhirEncounter = buildEncounter();

        List<FHIRResource> fhirResources = testResultMapper.map(obsService.getObs(101), new FHIREncounter(fhirEncounter), getSystemProperties("1"));

        assertNotNull(fhirResources);
        assertEquals(2, fhirResources.size());
        FHIRResource diagnosticReportResource = TestFhirFeedHelper.getFirstResourceByType(new DiagnosticReport().getResourceName(), fhirResources);

        assertDiagnosticReportWithResult(fhirResources, diagnosticReportResource, "Urea Nitorgen",
                "http://localhost:9997/openmrs/ws/rest/v1/tr/concept/501qb827-a67c-4q1f-a705-e5efe0qjki2w",
                "501qb827-a67c-4q1f-a705-e5efe0qjki2w",
                "http://localhost:9997/patients/hid/encounters/shrEncounterId1",
                null, "Lab Notes");
    }

    @Test
    public void shouldMapLabPanelResults() throws Exception {
        Encounter fhirEncounter = buildEncounter();
        List<FHIRResource> fhirResources = testResultMapper.map(obsService.getObs(11), new FHIREncounter(fhirEncounter), getSystemProperties("1"));
        assertNotNull(fhirResources);
        assertEquals(6, fhirResources.size());
        assertEquals(3, TestFhirFeedHelper.getResourceByType(new DiagnosticReport().getResourceName(), fhirResources).size());

        FHIRResource haemoglobinDiagnosticReport = getResourceByReference(new ResourceReferenceDt("urn:uuid:eee554cb-2afa-471a-9cd7-1434552c337c2"), fhirResources);
        assertNotNull(haemoglobinDiagnosticReport);
        assertDiagnosticReportWithResult(fhirResources, haemoglobinDiagnosticReport, "Haemoglobin",
                "http://localhost:9997/openmrs/ws/rest/v1/tr/concept/79647ed4-a60e-4cf5-ba68-cf4d55956cba",
                "79647ed4-a60e-4cf5-ba68-cf4d55956cba",
                "http://localhost:9997/patients/hid/encounters/shrEncounterId2",
                new QuantityDt(120.0), "Lab Notes");

        FHIRResource esrDiagnosticReport = getResourceByReference(new ResourceReferenceDt("urn:uuid:eee554cb-2afa-a7aa-9cd7-143455ac3a7c2"), fhirResources);
        assertNotNull(esrDiagnosticReport);
        assertDiagnosticReportWithResult(fhirResources, esrDiagnosticReport, "ESR",
                "http://localhost:9997/openmrs/ws/rest/v1/tr/concept/a04c36be-3f90-11e3-968c-0800271c1b75",
                "a04c36be-3f90-11e3-968c-0800271c1b75",
                "http://localhost:9997/patients/hid/encounters/shrEncounterId2",
                new QuantityDt(10.0), "Lab Notes");

        FHIRResource hbElectorphoresisDiagnosticReport = getResourceByReference(new ResourceReferenceDt("urn:uuid:aeea54cb-2afa-a7aa-9cd7-143455ac3a7c2"), fhirResources);
        assertNotNull(hbElectorphoresisDiagnosticReport);
        assertDiagnosticReportWithResult(fhirResources, hbElectorphoresisDiagnosticReport, "Hb Electrophoresis",
                "http://localhost:9997/openmrs/ws/rest/v1/tr/concept/da4bf386-3fcc-11e3-968c-0800271c1b75",
                "da4bf386-3fcc-11e3-968c-0800271c1b75",
                "http://localhost:9997/patients/hid/encounters/shrEncounterId2",
                new StringDt("yes"), null);

    }

    @Test
    public void shouldAssociateTestResultsAgainstOrderIfMappingPresent() throws Exception {
        Encounter fhirEncounter = buildEncounter();

        List<FHIRResource> fhirResources = testResultMapper.map(obsService.getObs(201), new FHIREncounter(fhirEncounter), getSystemProperties("1"));

        assertNotNull(fhirResources);
        assertEquals(2, fhirResources.size());
        FHIRResource diagnosticReportResource = TestFhirFeedHelper.getFirstResourceByType(new DiagnosticReport().getResourceName(), fhirResources);

        assertDiagnosticReportWithResult(fhirResources, diagnosticReportResource, "Urea Nitorgen",
                "http://localhost:9997/openmrs/ws/rest/v1/tr/concept/501qb827-a67c-4q1f-a705-e5efe0qjki2w",
                "501qb827-a67c-4q1f-a705-e5efe0qjki2w",
                "http://shr.com/patients/hid/encounters/shrEncounterId19#DiagnosticOrder/200ae386-20sx-4629-9850-f15206e63ab0",
                new QuantityDt(120.0), "Lab Notes");
    }

    private void assertDiagnosticReportWithResult(List<FHIRResource> fhirResources, FHIRResource reportResource, String display, String system, String code, String orderEncounterReference, IDatatype expectedObsValue, String expectedComments) {
        DiagnosticReport report = (DiagnosticReport) reportResource.getResource();
        assertDiagnosticReport(report, display, system, code, orderEncounterReference);
        assertEquals(1, report.getResult().size());
        FHIRResource observationResource = getResourceByReference(report.getResult().get(0), fhirResources);
        assertNotNull(observationResource);
        Observation observation = (Observation) observationResource.getResource();
        assertResultObservation(observation, expectedObsValue, expectedComments, display, system, code);
    }

    private void assertDiagnosticReport(DiagnosticReport report, String display, String system, String code, String requestReference) {
        assertEquals(patientHid, report.getSubject().getReference().getValue());
        assertEquals(providerId, report.getPerformer().getReference().getValue());
        assertEquals(DateUtil.parseDate("2008-08-18 15:09:05"), report.getIssued());
        assertEquals(DateUtil.parseDate("2008-08-08 00:00:00"), ((DateTimeDt) report.getEffective()).getValue());
        assertEquals(DiagnosticReportStatusEnum.FINAL.getCode(), report.getStatus());
        assertTrue(report.getRequest().get(0).getReference().getValue().startsWith(requestReference));
        assertEquals(1, report.getIdentifier().size());
        assertFalse(report.getIdentifier().get(0).isEmpty());
        assertFalse(report.getId().isEmpty());
        assertTrue(containsCoding(report.getCode().getCoding(), code, system, display));
        assertEquals(1, report.getCategory().getCoding().size());
        CodingDt categoryCoding = report.getCategory().getCodingFirstRep();
        assertEquals(FHIRProperties.FHIR_V2_VALUESET_DIAGNOSTIC_REPORT_CATEGORY_URL, categoryCoding.getSystem());
        assertEquals(FHIRProperties.FHIR_DIAGNOSTIC_REPORT_CATEGORY_LAB_CODE, categoryCoding.getCode());
        assertEquals(FHIRProperties.FHIR_DIAGNOSTIC_REPORT_CATEGORY_LAB_DISPLAY, categoryCoding.getDisplay());
    }

    private void assertResultObservation(Observation observation, IDatatype expectedObsValue, String expectedComments, String display, String system, String code) {
        assertEquals(1, observation.getIdentifier().size());
        assertFalse(observation.getIdentifier().get(0).isEmpty());
        assertFalse(observation.getId().isEmpty());
        assertEquals(ObservationStatusEnum.FINAL.getCode(), observation.getStatus());
        assertEquals(patientHid, observation.getSubject().getReference().getValue());
        assertEquals(encounterId, observation.getEncounter().getReference().getValue());
        assertEquals(1, observation.getPerformer().size());
        assertEquals(providerId, observation.getPerformer().get(0).getReference().getValue());
        assertEquals(expectedComments, observation.getComments());
        assertTrue(checkObservationValue(expectedObsValue, observation.getValue()));
        assertTrue(containsCoding(observation.getCode().getCoding(), code, system, display));
    }

    private boolean checkObservationValue(IDatatype expectedObsValue, IDatatype actualObsValue) {
        if (expectedObsValue == null && actualObsValue == null) return true;
        if (expectedObsValue instanceof QuantityDt && actualObsValue instanceof QuantityDt)
            return ((QuantityDt) expectedObsValue).getValue().doubleValue() == ((QuantityDt) actualObsValue).getValue().doubleValue();
        if (expectedObsValue instanceof StringDt && actualObsValue instanceof StringDt)
            return ((StringDt) expectedObsValue).getValue().equals(((StringDt) actualObsValue).getValue());
        return false;
    }

    private Encounter buildEncounter() {
        Encounter fhirEncounter = new Encounter();
        fhirEncounter.setId(encounterId);
        fhirEncounter.setIdentifier(asList(encounterId));
        fhirEncounter.setPatient(new ResourceReferenceDt().setReference(patientHid));
        Encounter.Participant participant = fhirEncounter.addParticipant();
        participant.setIndividual(new ResourceReferenceDt().setReference(providerId));
        return fhirEncounter;
    }
}