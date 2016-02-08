package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.resource.Procedure;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticReportStatusEnum;
import ca.uhn.fhir.model.primitive.StringDt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir.TestFhirFeedHelper;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.containsCoding;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;
import static org.openmrs.module.fhir.TestFhirFeedHelper.getFirstResourceByType;
import static org.openmrs.module.fhir.TestFhirFeedHelper.getResourceByReference;
import static org.openmrs.module.fhir.utils.DateUtil.parseDate;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProcedureFulfillmentMapperIT extends BaseModuleWebContextSensitiveTest {
    @Autowired
    private ProcedureFulfillmentMapper procedureFulfillmentMapper;
    @Autowired
    private ObsService obsService;

    private final String patientRef = "http://mci.com/patients/HID-123";
    private final String fhirEncounterId = "SHR-ENC1";

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/procedureFulfillmentDS.xml");
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldHandleProcedureOrderFulfillment() throws Exception {
        Obs fulfilmentObs = obsService.getObs(1011);
        assertTrue(procedureFulfillmentMapper.canHandle(fulfilmentObs));
    }

    @Test
    public void shouldNotHandleIfFulfilmentIsNotHavingProcedureTemplate() throws Exception {
        Obs fulfilmentObs = obsService.getObs(1000);
        assertFalse(procedureFulfillmentMapper.canHandle(fulfilmentObs));
    }

    @Test
    public void shouldMapAProcedureOrderFulfillment() throws Exception {
        Obs obs = obsService.getObs(1011);
        List<FHIRResource> resources = procedureFulfillmentMapper.map(obs, createFhirEncounter(), getSystemProperties("1"));

        assertFalse(resources.isEmpty());
        assertEquals(1, resources.size());
        IResource resource = resources.get(0).getResource();
        assertTrue(resource instanceof Procedure);
        Procedure procedure = (Procedure) resource;

        assertProcedure(obs, procedure);
    }

    @Test
    public void shouldNotMapIfProcedureTypeIsNotPresent() throws Exception {
        Obs fullfilmentObs = obsService.getObs(1499);
        List<FHIRResource> resources = procedureFulfillmentMapper.map(fullfilmentObs, createFhirEncounter(), getSystemProperties("1"));

        assertTrue(resources.isEmpty());
    }

    @Test
    public void shouldMapDiagnosisStudy() throws Exception {
        Obs fulfillmentObs = obsService.getObs(1099);
        List<FHIRResource> resources = procedureFulfillmentMapper.map(fulfillmentObs, createFhirEncounter(), getSystemProperties("1"));

        assertEquals(3, resources.size());
        Procedure procedure = (Procedure) getFirstResourceByType(new Procedure().getResourceName(), resources).getResource();
        assertProcedure(fulfillmentObs, procedure);

        List<ResourceReferenceDt> reports = procedure.getReport();
        assertEquals(1, reports.size());
        ResourceReferenceDt reportReference = reports.get(0);
        DiagnosticReport report = (DiagnosticReport) TestFhirFeedHelper.getResourceByReference(reportReference, resources).getResource();

        assertDiagnosticReport(report);

        List<ResourceReferenceDt> reportResult = report.getResult();
        assertEquals(1, reportResult.size());
        ResourceReferenceDt resultReference = reportResult.get(0);
        Observation result = (Observation) getResourceByReference(resultReference, resources).getResource();

        assertProcedureResult(result);
    }

    public void assertProcedureResult(Observation result) {
        assertEquals("dia574cb-22yy-671a-giz7-3450552cresult", result.getId().getIdPart());
        assertTrue(containsCoding(result.getCode().getCoding(), "Test A-LOINC", "http://tr.com/Test-A-LOINC", "Test A"));
        assertEquals("Blood Pressure is very high", ((StringDt) result.getValue()).getValue());
    }

    public void assertDiagnosticReport(DiagnosticReport report) {
        assertEquals(fhirEncounterId, report.getEncounter().getReference().getValue());
        assertEquals(patientRef, report.getSubject().getReference().getValue());
        assertEquals(DiagnosticReportStatusEnum.FINAL.getCode(), report.getStatus());
        assertEquals(parseDate("2010-08-01 15:09:05"), report.getIssued());
        assertEquals("ew6574cb-22yy-891a-giz7-3450552c77459", report.getId().getIdPart());
        List<CodingDt> reportCoding = report.getCode().getCoding();
        assertTrue(containsCoding(reportCoding, "Test A-LOINC", "http://tr.com/Test-A-LOINC", "Test A"));
        List<CodingDt> procedureDiagnosisCoding = report.getCodedDiagnosis().get(0).getCoding();
        assertTrue(containsCoding(procedureDiagnosisCoding, "J19.406475", "http://tr.com/Viral-Pneumonia-LOINC", "Viral pneumonia 406475"));
    }

    private void assertProcedure(Obs obs, Procedure procedure) {
        assertNotNull(procedure.getIdentifier());
        assertNotNull(procedure.getId());
        assertEquals(obs.getUuid(), procedure.getId().getIdPart());
        assertEquals(patientRef, procedure.getSubject().getReference().getValue());
        assertEquals(fhirEncounterId, procedure.getEncounter().getReference().getValue());
        List<CodingDt> procedureCode = procedure.getCode().getCoding();
        String procedureTypeCode = "Osteopathic-Treatment-of-Abdomen";
        String procedureTypeSystem = "http://tr.com/Osteopathic-Treatment-of-Abdomen";
        assertTrue(containsCoding(procedureCode, procedureTypeCode, procedureTypeSystem, "ProcedureAnswer1"));
        List<CodingDt> procedureOutcome = procedure.getOutcome().getCoding();
        assertTrue(containsCoding(procedureOutcome, "385669000", "http://localhost:9080/openmrs/ws/rest/v1/tr/vs/Procedure-Outcome", "Successful"));
        List<CodingDt> procedureFollowup = procedure.getFollowUpFirstRep().getCoding();
        assertTrue(containsCoding(procedureFollowup, "6831", "http://tr.com/6831", "Change of dressing"));
        assertEquals("in-progress", procedure.getStatus());
        assertEquals("Procedure went well", procedure.getNotesFirstRep().getText());
        PeriodDt performed = (PeriodDt) procedure.getPerformed();
        assertEquals(parseDate("2015-01-10 00:00:00"), performed.getStart());
        assertEquals(parseDate("2015-01-15 00:00:00"), performed.getEnd());
        assertEquals("http://shr.com/patients/HID/encounters/shr-enc-1#ProcedureRequest/procedure_req_id", procedure.getRequest().getReference().getValue());
    }

    private FHIREncounter createFhirEncounter() {
        ca.uhn.fhir.model.dstu2.resource.Encounter encounter = new ca.uhn.fhir.model.dstu2.resource.Encounter();
        encounter.setPatient(new ResourceReferenceDt(patientRef));
        encounter.setId(fhirEncounterId);
        return new FHIREncounter(encounter);
    }

}