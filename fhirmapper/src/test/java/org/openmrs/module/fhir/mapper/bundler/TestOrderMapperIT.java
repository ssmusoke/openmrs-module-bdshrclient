package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import ca.uhn.fhir.model.dstu2.resource.Specimen;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.module.fhir.TestFhirFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TestOrderMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    TestOrderMapper testOrderMapper;

    @Autowired
    EncounterService encounterService;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldMapTestOrderForAPanelOrTest() throws Exception {
        Encounter encounter = encounterService.getEncounter(36);
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = new ca.uhn.fhir.model.dstu2.resource.Encounter();
        assertEquals(1, encounter.getOrders().size());
        Bundle bundle = new Bundle();
        Order order = encounter.getOrders().iterator().next();
        List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, bundle, getSystemProperties("1"));
        assertNotNull(mappedResources);
        assertEquals(2, mappedResources.size());
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder)TestFhirFeedHelper.getResourceByType(new DiagnosticOrder().getResourceName(), mappedResources).getResource();
        assertNotNull(diagnosticOrder);
        assertNotNull(TestFhirFeedHelper.getResourceByType(new Specimen().getResourceName(), mappedResources));
        assertTrue(diagnosticOrder.getOrderer().getReference().getValue().endsWith("321.json"));
    }

    @Test
    public void shouldMapTestOrderWithoutLoincName() throws Exception {
        Encounter encounter = encounterService.getEncounter(38);
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = new ca.uhn.fhir.model.dstu2.resource.Encounter();
        assertEquals(2, encounter.getOrders().size());
        Bundle bundle = new Bundle();
        Order order = encounter.getOrders().iterator().next();
        List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, bundle, getSystemProperties("1"));
        assertNotNull(mappedResources);
        assertEquals(1, mappedResources.size());
    }

    @Test
    public void shouldMapTestOrderForATestAndPanelToSameDiagnosticOrder() throws Exception {
        Encounter encounter = encounterService.getEncounter(38);
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = new ca.uhn.fhir.model.dstu2.resource.Encounter();
        assertEquals(2, encounter.getOrders().size());
        Bundle bundle = new Bundle();
        for (Order order : encounter.getOrders()) {
            List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, bundle, getSystemProperties("1"));
            assertNotNull(mappedResources);
            addToAtomFeed(bundle, mappedResources);
        }
        assertEquals(1, bundle.getEntry().size());
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) bundle.getEntry().get(0).getResource();
        assertEquals(2, diagnosticOrder.getItem().size());
    }

    @Test
    public void shouldNotAddSpecimenIfAlreadyPresentForSameAccession() throws Exception {
        Encounter encounter = encounterService.getEncounter(39);
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = new ca.uhn.fhir.model.dstu2.resource.Encounter();
        assertEquals(2, encounter.getOrders().size());
        Bundle bundle = new Bundle();
        for (Order order : encounter.getOrders()) {
            List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, bundle, getSystemProperties("1"));
            assertNotNull(mappedResources);
            addToAtomFeed(bundle, mappedResources);
        }
        assertEquals(2, bundle.getEntry().size());
    }

    @Test
    public void shouldAddSpecimenIfAlreadyPresentForSameAccession() throws Exception {
        Encounter encounter = encounterService.getEncounter(40);
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = new ca.uhn.fhir.model.dstu2.resource.Encounter();
        assertEquals(2, encounter.getOrders().size());
        Bundle bundle = new Bundle();
        for (Order order : encounter.getOrders()) {
            List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, bundle, getSystemProperties("1"));
            assertNotNull(mappedResources);
            addToAtomFeed(bundle, mappedResources);
        }
        assertEquals(3, bundle.getEntry().size());
    }

    @SuppressWarnings("unchecked")
    private void addToAtomFeed(Bundle bundle, List<FHIRResource> mappedResources) {
        for (FHIRResource resource : mappedResources) {
            Bundle.Entry resourceEntry = new Bundle.Entry();
            resourceEntry.setResource(resource.getResource());
            bundle.addEntry(resourceEntry);
        }
    }
}