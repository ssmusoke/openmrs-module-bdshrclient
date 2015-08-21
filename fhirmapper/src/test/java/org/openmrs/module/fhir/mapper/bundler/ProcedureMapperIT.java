package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Procedure;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import org.apache.commons.collections.CollectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;
import static org.openmrs.module.fhir.TestFhirFeedHelper.getResourceByReference;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProcedureMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    ObsService obsService;

    @Autowired
    private ProcedureMapper procedureMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/procedureDS.xml");
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldHandleProcedureTypeObservation() {
        Obs observation = obsService.getObs(1100);
        assertTrue(procedureMapper.canHandle(observation));
    }

    @Test
    public void shouldNotMapIfProcedureTypeIsNotPresent() throws Exception {
        Obs obs = obsService.getObs(1500);
        List<FHIRResource> fhirResources = procedureMapper.map(obs, new Encounter(), getSystemProperties("1"));
        assertTrue(CollectionUtils.isEmpty(fhirResources));
    }

    @Test
    public void shouldNotMapDiagnosisReportIfDiagnosticTestIsNotPresent() throws Exception {
        Obs obs = obsService.getObs(1501);
        List<FHIRResource> fhirResources = procedureMapper.map(obs, new Encounter(), getSystemProperties("1"));
        assertTrue(fhirResources.size() == 1);
        assertTrue(fhirResources.get(0).getResource() instanceof Procedure);
    }

    @Test
    public void shouldMapPatient() {
        Encounter fhirEncounter = new Encounter();
        ResourceReferenceDt patient = new ResourceReferenceDt();
        patient.setReference("Hid");
        fhirEncounter.setPatient(patient);

        Procedure procedure = getProcedure(mapProcedure(1100, fhirEncounter));

        assertEquals(patient, procedure.getPatient());
    }

//    @Test
//    public void shouldMapOutCome(){
//        Procedure procedure= getProcedure(mapProcedure(1100, new Encounter()));
//        assertEquals("Outcome results",procedure.getOutcome().getValue());
//    }

//    @Test
//    public void shouldMapFollowUp() throws Exception {
//        Procedure procedure= getProcedure(mapProcedure(1100, new Encounter()));
//        assertEquals("Follow up actions",procedure.getFollowUp().getValue());
//    }

    @Test
    public void shouldMapProcedure() throws Exception {
        Procedure procedure = getProcedure(mapProcedure(1100, new Encounter()));
        CodingDt procedureType = procedure.getType().getCoding().get(0);
        assertNotNull(procedureType);
        assertEquals("ProcedureAnswer1", procedureType.getDisplay());
        assertEquals("http://tr.com/Osteopathic-Treatment-of-Abdomen", procedureType.getSystem());
        assertEquals("Osteopathic-Treatment-of-Abdomen", procedureType.getCode());
    }

    @Test
    public void shouldMapPeriod() throws Exception {
        Procedure procedure = getProcedure(mapProcedure(1100, new Encounter()));
        PeriodDt period = (PeriodDt) procedure.getPerformed();

        Date expectedStartDate = DateUtil.parseDate("2015-01-10 00:00:00");
        Date expectedEndDate = DateUtil.parseDate("2015-01-15 00:00:00");

        assertEquals(expectedStartDate, period.getStart());
        assertEquals(expectedEndDate, period.getEnd());
    }

    //TODO: Set proper data
    @Test
    public void shouldMapReferenceToDiagnosisReport() throws Exception {
        Encounter encounter = buildEncounter();
        List<FHIRResource> fhirResources = mapProcedure(1100, encounter);
        Procedure procedure = getProcedure(fhirResources);

        ResourceReferenceDt resourceReference = procedure.getReport().get(0);
        IResource dianosticReportResource = getResourceByReference(resourceReference, fhirResources).getResource();
        DiagnosticReport diagnosticReport = null;
        if (dianosticReportResource instanceof DiagnosticReport) {
            diagnosticReport = (DiagnosticReport) dianosticReportResource;
        }
        assertNotNull(diagnosticReport);

        assertEquals("patient", diagnosticReport.getSubject().getReference().getValue());
        assertEquals("Provider 1", diagnosticReport.getPerformer().getReference().getValue());
        Date expectedDate = DateUtil.parseDate("2010-08-18 15:09:05");
        Date diagnosticDate = ((DateTimeDt) diagnosticReport.getDiagnostic()).getValue();
        assertEquals(expectedDate, diagnosticDate);

        assertDiagnosticName(diagnosticReport);
        assertCodedDiagnosis(diagnosticReport);
        assertDiagnosisResult(diagnosticReport);

    }

    private void assertDiagnosisResult(DiagnosticReport diagnosticReport) {
        ResourceReferenceDt resourceRefResult = diagnosticReport.getResult().get(0);
        assertEquals("Blood Pressure is very high", resourceRefResult.getDisplay().getValue());
    }

    private void assertCodedDiagnosis(DiagnosticReport diagnosticReport) {
        assertTrue(diagnosticReport.getCodedDiagnosis().size() == 1);
        List<CodingDt> codings = diagnosticReport.getCodedDiagnosis().get(0).getCoding();
        assertTrue(codings.size() == 2);

        CodingDt referenceTermCoding = codings.get(0);
        assertEquals("http://tr.com/Viral-Pneumonia-LOINC", referenceTermCoding.getSystem());
        assertEquals("Viral pneumonia 406475", referenceTermCoding.getDisplay());
        assertEquals("J19.406475", referenceTermCoding.getCode());

        CodingDt termCoding = codings.get(1);
        assertEquals("http://tr.com/Viral-Pneumonia", termCoding.getSystem());
        assertEquals("Viral pneumonia 406475", termCoding.getDisplay());
        assertEquals("Viral-Pneumonia", termCoding.getCode());
    }

    private void assertDiagnosticName(DiagnosticReport diagnosticReport) {
        List<CodingDt> codings = diagnosticReport.getName().getCoding();
        assertTrue(codings.size() == 2);

        CodingDt referenceTermCoding = codings.get(0);
        assertEquals("Test A", referenceTermCoding.getDisplay());
        assertEquals("http://tr.com/Test-A-LOINC", referenceTermCoding.getSystem());
        assertEquals("Test A-LOINC", referenceTermCoding.getCode());

        CodingDt termCoding = codings.get(1);
        assertEquals("Test A", termCoding.getDisplay());
        assertEquals("http://tr.com/Test-A", termCoding.getSystem());
        assertEquals("Test A", termCoding.getCode());
    }

    private List<FHIRResource> mapProcedure(int observationId, Encounter fhirEncounter) {
        Obs obs = obsService.getObs(observationId);
        List<FHIRResource> fhirResources = procedureMapper.map(obs, fhirEncounter, getSystemProperties("1"));

        assertEquals(2, fhirResources.size());

        assertTrue(fhirResources.get(0).getResource() instanceof DiagnosticReport);
        assertEquals("urn:uuid:ew6574cb-22yy-891a-giz7-3450552c77459", fhirResources.get(0).getIdentifierList().get(0).getValue());
        assertEquals("Diagnostic Report", fhirResources.get(0).getResourceName());

        assertTrue(fhirResources.get(1).getResource() instanceof Procedure);
        assertEquals("urn:uuid:ef4554cb-22gg-471a-lld7-1434552c337c1", fhirResources.get(1).getIdentifierList().get(0).getValue());
        assertEquals(MRSProperties.MRS_CONCEPT_PROCEDURES_TEMPLATE, fhirResources.get(1).getResourceName());

        return fhirResources;
    }

    private Procedure getProcedure(List<FHIRResource> fhirResources) {
        IResource procedure = null;
        for (FHIRResource fhirResource : fhirResources) {
            if ((procedure = fhirResource.getResource()) instanceof Procedure) {
                return (Procedure) procedure;
            }
        }
        return null;
    }

    private Encounter buildEncounter() {
        Encounter fhirEncounter = new Encounter();
        fhirEncounter.setPatient(new ResourceReferenceDt().setReference("patient"));
        Encounter.Participant participant = fhirEncounter.addParticipant();
        participant.setIndividual(new ResourceReferenceDt().setReference("Provider 1"));

        return fhirEncounter;
    }
}
