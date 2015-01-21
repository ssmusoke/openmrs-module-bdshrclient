package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Period;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class ProcedureMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    ObsService obsService;

    @Autowired
    private ProcedureMapper procedureMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/procedureDS.xml");
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
    public void shouldMapSubject() {
        Encounter fhirEncounter = new Encounter();
        ResourceReference subject = new ResourceReference();
        subject.setReferenceSimple("Hid");
        fhirEncounter.setSubject(subject);

        Procedure procedure= getProcedure(mapProcedure(1100, fhirEncounter));

        assertEquals(subject, procedure.getSubject());
    }

    @Test
    public void shouldMapOutCome(){
        Procedure procedure= getProcedure(mapProcedure(1100, new Encounter()));
        assertEquals("Outcome results",procedure.getOutcome().getValue());
    }

    @Test
    public void shouldMapFollowUp() throws Exception {
        Procedure procedure= getProcedure(mapProcedure(1100, new Encounter()));
        assertEquals("Follow up actions",procedure.getFollowUp().getValue());

    }

    @Test
    public void shouldMapProcedure() throws Exception {
        Procedure procedure= getProcedure(mapProcedure(1100, new Encounter()));
        Coding procedureType = procedure.getType().getCoding().get(0);
        assertNotNull(procedureType);
        assertEquals("ProcedureAnswer1",procedureType.getDisplaySimple());
        assertEquals("http://tr.com/Osteopathic-Treatment-of-Abdomen",procedureType.getSystemSimple());
        assertEquals("Osteopathic-Treatment-of-Abdomen",procedureType.getCodeSimple());
    }

    @Test
    public void shouldMapPeriod() throws Exception {
        Procedure procedure= getProcedure(mapProcedure(1100, new Encounter()));
        Period period = procedure.getDate();

        assertEquals("2015-01-10T00:00:00+05:30", period.getStart().getValue().toString());
        assertEquals("2015-01-15T00:00:00+05:30", period.getEnd().getValue().toString());
    }

    //TODO: Set proper data
    @Test
    public void shouldMapReferenceToDiagnosisReport() throws Exception {
        Encounter encounter= buildEncounter();
        List<FHIRResource> fhirResources= mapProcedure(1100,encounter);
        Procedure procedure= getProcedure(fhirResources);

        ResourceReference resourceReference = procedure.getReport().get(0);
        Resource dianosticReportResource= getResource(resourceReference,fhirResources).getResource();
        DiagnosticReport diagnosticReport= null;
        if (dianosticReportResource instanceof DiagnosticReport){
            diagnosticReport= (DiagnosticReport) dianosticReportResource;
        }
        assertNotNull(diagnosticReport);

        assertEquals("patient",diagnosticReport.getSubject().getReferenceSimple());
        assertEquals("Provider 1",diagnosticReport.getPerformer().getReferenceSimple());
        assertEquals("2010-08-18T15:09:05+05:30",((DateTime)diagnosticReport.getDiagnostic()).getValue().toString());

        assertDiagnosticName(diagnosticReport);
        assertCodedDiagnosis(diagnosticReport);
        assertDiagnosisResult(diagnosticReport);

    }

    private void assertDiagnosisResult(DiagnosticReport diagnosticReport) {
        ResourceReference resourceRefResult = diagnosticReport.getResult().get(0);
        assertEquals("Blood Pressure is very high",resourceRefResult.getDisplaySimple());

    }

    private void assertCodedDiagnosis(DiagnosticReport diagnosticReport) {
        assertTrue(diagnosticReport.getCodedDiagnosis().size() == 1);
        List<Coding> codings = diagnosticReport.getCodedDiagnosis().get(0).getCoding();
        assertTrue(codings.size() == 2);

        Coding referenceTermCoding= codings.get(0);
        assertEquals("http://tr.com/Viral-Pneumonia-LOINC", referenceTermCoding.getSystemSimple());
        assertEquals("Viral pneumonia 406475", referenceTermCoding.getDisplaySimple());
        assertEquals("J19.406475",referenceTermCoding.getCodeSimple());

        Coding termCoding= codings.get(1);
        assertEquals("http://tr.com/Viral-Pneumonia", termCoding.getSystemSimple());
        assertEquals("Viral pneumonia 406475", termCoding.getDisplaySimple());
        assertEquals("Viral-Pneumonia",termCoding.getCodeSimple());
    }

    private void assertDiagnosticName(DiagnosticReport diagnosticReport) {
        List<Coding> codings = diagnosticReport.getName().getCoding();
        assertTrue(codings.size()== 2);

        Coding referenceTermCoding= codings.get(0);
        assertEquals("Test A", referenceTermCoding.getDisplaySimple());
        assertEquals("http://tr.com/Test-A-LOINC", referenceTermCoding.getSystemSimple());
        assertEquals("Test A-LOINC",referenceTermCoding.getCodeSimple());

        Coding termCoding= codings.get(1);
        assertEquals("Test A", termCoding.getDisplaySimple());
        assertEquals("http://tr.com/Test-A", termCoding.getSystemSimple());
        assertEquals("Test A",termCoding.getCodeSimple());
    }

    private List<FHIRResource> mapProcedure(int observationId, Encounter fhirEncounter) {
        Obs obs = obsService.getObs(observationId);
        List<FHIRResource> fhirResources = procedureMapper.map(obs, fhirEncounter, getSystemProperties("1"));

        assertEquals(2, fhirResources.size());

        assertTrue(fhirResources.get(0).getResource() instanceof DiagnosticReport);
        assertEquals("urn:ew6574cb-22yy-891a-giz7-3450552c77459", fhirResources.get(0).getIdentifierList().get(0).getValueSimple());
        assertEquals("Diagnostic Report", fhirResources.get(0).getResourceName());

        assertTrue(fhirResources.get(1).getResource() instanceof Procedure);
        assertEquals("urn:ef4554cb-22gg-471a-lld7-1434552c337c1", fhirResources.get(1).getIdentifierList().get(0).getValueSimple());
        assertEquals("Procedures", fhirResources.get(1).getResourceName());

        return fhirResources;
    }

    private Procedure getProcedure(List<FHIRResource> fhirResources){
        Resource procedure= null;
        for (FHIRResource fhirResource : fhirResources) {
             if((procedure= fhirResource.getResource()) instanceof Procedure){
                 return (Procedure)procedure;
             }
        }
        return null;
    }

    private FHIRResource getResource(ResourceReference reference, List<FHIRResource> FHIRResources) {
        for (FHIRResource FHIRResource : FHIRResources) {
            if(FHIRResource.getIdentifier().getValueSimple().equals(reference.getReferenceSimple())) {
                return FHIRResource;
            }
        }
        return null;
    }

    private Encounter buildEncounter(){
        Encounter fhirEncounter = new Encounter();
        ResourceReference subject = new ResourceReference();
        subject.setReferenceSimple("patient");
        fhirEncounter.setSubject(subject);
        Encounter.EncounterParticipantComponent participant = fhirEncounter.addParticipant();
        ResourceReference individual = new ResourceReference();
        individual.setReferenceSimple("Provider 1");
        participant.setIndividual(individual);

        return fhirEncounter;
    }


}
