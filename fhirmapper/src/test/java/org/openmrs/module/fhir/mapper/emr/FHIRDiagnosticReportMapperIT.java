package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.DiagnosticOrder;
import org.hl7.fhir.instance.model.DiagnosticReport;
import org.hl7.fhir.instance.model.Observation;
import org.hl7.fhir.instance.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.api.ConceptService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.module.fhir.TestHelper;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.hl7.fhir.instance.model.ResourceType.*;
import static org.junit.Assert.*;

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
    private FHIRDiagnosticOrderMapper diagnosticOrderMapper;
    @Autowired
    private FHIRObservationsMapper observationsMapper;
    @Autowired
    private OrderService orderService;

    @Before
    public void setUp() throws Exception {
        executeDataSet("labResultDS.xml");
    }

    @Test
    public void shouldMapDiagnosticReportForTestResult() throws Exception {
        AtomFeed bundle = new TestHelper()
                .loadSampleFHIREncounter("classpath:encounterBundles/encounterWithDiagnosticReport.xml", springContext)
                .getFeed();
        DiagnosticReport report = (org.hl7.fhir.instance.model.DiagnosticReport) FHIRFeedHelper.identifyResource(bundle.getEntryList(), DiagnosticReport);
        Encounter encounter = new Encounter();
        encounter.setPatient(patientService.getPatient(1));
        HashMap<String, List<String>> processedList = new HashMap<>();
        diagnosticReportMapper.map(bundle, report, encounter.getPatient(), encounter, processedList);
        assertEquals(2, processedList.size());
        assertTrue(processedList.containsKey(report.getIdentifier().getValueSimple()));
        Set<Obs> obsSet = encounter.getObsAtTopLevel(false);
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
        assertEquals(Double.valueOf(3000), resultObs.getValueNumeric());

        Obs notesObs = findObsByConcept(secondLevelObs.getGroupMembers(), conceptService.getConcept(103));
        assertNotNull(notesObs);
        assertEquals(testOrder, notesObs.getOrder());
        assertEquals("notes: hello world", notesObs.getValueText());
    }

    @Test
    public void shouldAddAlreadyProcessedObservationResultToTestResults() throws Exception {
        AtomFeed bundle = new TestHelper()
                .loadSampleFHIREncounter("classpath:encounterBundles/encounterWithDiagnosticReport.xml", springContext)
                .getFeed();
        DiagnosticReport report = (org.hl7.fhir.instance.model.DiagnosticReport) FHIRFeedHelper.identifyResource(bundle.getEntryList(), DiagnosticReport);
        org.hl7.fhir.instance.model.Observation observation = (Observation) FHIRFeedHelper.identifyResource(bundle.getEntryList(), Observation);
        Encounter encounter = new Encounter();
        encounter.setPatient(patientService.getPatient(1));
        HashMap<String, List<String>> processedList = new HashMap<>();

        observationsMapper.map(bundle, observation, encounter.getPatient(), encounter, processedList);
        assertTrue(processedList.containsKey(observation.getIdentifier().getValueSimple()));

        diagnosticReportMapper.map(bundle, report, encounter.getPatient(), encounter, processedList);
        assertEquals(2, processedList.size());
        assertTrue(processedList.containsKey(report.getIdentifier().getValueSimple()));
        Set<Obs> obsSet = encounter.getObsAtTopLevel(false);
        assertEquals(1, obsSet.size());
        Concept concept = conceptService.getConcept(303);
        assertTestObs(obsSet.iterator().next(), concept);
    }

    @Test
    public void shouldAddAlreadyProcessedObservationResultToPanelResults() throws Exception {
        AtomFeed bundle = new TestHelper()
                .loadSampleFHIREncounter("classpath:encounterBundles/encounterWithPanelReport.xml", springContext)
                .getFeed();
        List<Resource> resources = FHIRFeedHelper.identifyResources(bundle.getEntryList(), DiagnosticReport);
        Encounter encounter = new Encounter();
        encounter.setPatient(patientService.getPatient(1));
        HashMap<String, List<String>> processedList = new HashMap<>();
        for (Resource resource : resources) {
            org.hl7.fhir.instance.model.DiagnosticReport report = (org.hl7.fhir.instance.model.DiagnosticReport) resource;
            diagnosticReportMapper.map(bundle, report, encounter.getPatient(), encounter, processedList);
            assertTrue(processedList.containsKey(report.getIdentifier().getValueSimple()));
        }
        assertEquals(4, processedList.size());
        Set<Obs> obsSet = encounter.getObsAtTopLevel(false);
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

    @Test
    public void shouldMapDiagnosticReportWithInternalReferenceToDiagnosticOrder() throws Exception {
        AtomFeed bundle = new TestHelper()
                .loadSampleFHIREncounter("classpath:encounterBundles/encounterWithDiagnosticOrderAndDiagnosticReport.xml", springContext)
                .getFeed();
        DiagnosticOrder order = (DiagnosticOrder) FHIRFeedHelper.identifyResource(bundle.getEntryList(), DiagnosticOrder);
        DiagnosticReport report = (org.hl7.fhir.instance.model.DiagnosticReport) FHIRFeedHelper.identifyResource(bundle.getEntryList(), DiagnosticReport);
        Encounter encounter = new Encounter();
        encounter.setPatient(patientService.getPatient(1));
        HashMap<String, List<String>> processedList = new HashMap<>();

        diagnosticReportMapper.map(bundle, report, encounter.getPatient(), encounter, processedList);
        assertEquals(3, processedList.size());

        assertTrue(processedList.containsKey(order.getIdentifier().get(0).getValueSimple()));
        assertEquals(1, encounter.getOrders().size());

        assertTrue(processedList.containsKey(report.getIdentifier().getValueSimple()));
        Set<Obs> obsSet = encounter.getObsAtTopLevel(false);
        assertEquals(1, obsSet.size());
        Concept concept = conceptService.getConcept(303);
        assertTestObs(obsSet.iterator().next(), concept);
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