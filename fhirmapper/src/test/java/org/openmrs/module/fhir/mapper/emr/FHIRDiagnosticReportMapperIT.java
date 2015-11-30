package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.api.ConceptService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.mapper.model.EmrEncounter;
import org.openmrs.module.fhir.mapper.model.ShrEncounter;
import org.openmrs.module.fhir.utils.FHIRBundleHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class FHIRDiagnosticReportMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private FHIRDiagnosticReportMapper diagnosticReportMapper;
    @Autowired
    private FHIRObservationsMapper observationsMapper;
    @Autowired
    private OrderService orderService;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/labResultDS.xml");
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }


    @Test
    public void shouldMapDiagnosticReportForTestResult() throws Exception {
        Bundle bundle = (Bundle) new MapperTestHelper()
                .loadSampleFHIREncounter("encounterBundles/dstu2/encounterWithDiagnosticReport.xml", springContext);
        DiagnosticReport report = (DiagnosticReport) FHIRBundleHelper.identifyResource(bundle.getEntry(), new DiagnosticReport().getResourceName());
        Encounter encounter = new Encounter();
        EmrEncounter emrEncounter = new EmrEncounter(encounter);
        encounter.setPatient(patientService.getPatient(1));

        ShrEncounter encounterComposition = new ShrEncounter(bundle, "98101039678", "shr-enc-id-1");
        diagnosticReportMapper.map(report, emrEncounter, encounterComposition, getSystemProperties("1"));
        Set<Obs> obsSet = emrEncounter.getTopLevelObs();
        assertEquals(1, obsSet.size());
        Obs topLevelObs = obsSet.iterator().next();
        Concept hemoglobinConcept = conceptService.getConcept(303);
        assertEquals(hemoglobinConcept, topLevelObs.getConcept());
        Order testOrder = orderService.getOrder(50);
        assertEquals(testOrder, topLevelObs.getOrder());
        assertEquals(1, topLevelObs.getGroupMembers().size());

        Obs secondLevelObs = topLevelObs.getGroupMembers().iterator().next();
        assertEquals(hemoglobinConcept, secondLevelObs.getConcept());
        assertEquals(testOrder, secondLevelObs.getOrder());
        assertEquals(2, secondLevelObs.getGroupMembers().size());

        Obs resultObs = findObsByConcept(secondLevelObs.getGroupMembers(), hemoglobinConcept);
        assertNotNull(resultObs);
        assertEquals(testOrder, resultObs.getOrder());
        assertEquals(Double.valueOf(20), resultObs.getValueNumeric());

        Concept labNotesConcept = conceptService.getConcept(103);
        Obs notesObs = findObsByConcept(secondLevelObs.getGroupMembers(), labNotesConcept);
        assertNotNull(notesObs);
        assertEquals(testOrder, notesObs.getOrder());
        assertEquals("changed", notesObs.getValueText());
    }

    @Test
    public void shouldProcessPanelResults() throws Exception {
        Bundle bundle = (Bundle) new MapperTestHelper().loadSampleFHIREncounter("encounterBundles/dstu2/encounterWithPanelReport.xml", springContext);
        List<IResource> resources = FHIRBundleHelper.identifyResourcesByName(bundle.getEntry(), new DiagnosticReport().getResourceName());
        Encounter encounter = new Encounter();
        EmrEncounter emrEncounter = new EmrEncounter(encounter);
        encounter.setPatient(patientService.getPatient(1));
        for (IResource resource : resources) {
            DiagnosticReport report = (DiagnosticReport) resource;
            ShrEncounter encounterComposition = new ShrEncounter(bundle, "98101039678", "shr-enc-id-1");
            diagnosticReportMapper.map(report, emrEncounter, encounterComposition, getSystemProperties("1"));
        }
        Set<Obs> obsSet = emrEncounter.getTopLevelObs();
        assertEquals(1, obsSet.size());
        Obs panelObs = obsSet.iterator().next();
        Concept panelConcept = conceptService.getConcept(302);
        assertEquals(panelConcept, panelObs.getConcept());
        Order testOrder = orderService.getOrder(51);
        assertEquals(testOrder, panelObs.getOrder());
        assertEquals(2, panelObs.getGroupMembers().size());

        Concept hemoglobinConcept = conceptService.getConcept(303);
        Obs hemoglobinObs = findObsByConcept(panelObs.getGroupMembers(), hemoglobinConcept);
        assertNotNull(hemoglobinObs);
        assertTestObs(hemoglobinObs, hemoglobinConcept);

        Concept esrConcept = conceptService.getConcept(304);
        Obs esrObs = findObsByConcept(panelObs.getGroupMembers(), esrConcept);
        assertNotNull(esrObs);
        assertTestObs(esrObs, esrConcept);
    }

    private void assertTestObs(Obs obs, Concept concept) {
        assertEquals(1, obs.getGroupMembers().size());

        Obs secondLevelObs = obs.getGroupMembers().iterator().next();
        assertEquals(2, secondLevelObs.getGroupMembers().size());

        Obs resultObs = findObsByConcept(secondLevelObs.getGroupMembers(), concept);
        assertNotNull(resultObs);
    }

    private Obs findObsByConcept(Set<Obs> obsSet, Concept concept) {
        for (Obs obs : obsSet) {
            if (obs.getConcept().equals(concept)) {
                return obs;
            }
        }
        return null;
    }
}