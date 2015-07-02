package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.AtomEntry;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.DiagnosticOrder;
import org.hl7.fhir.instance.model.ResourceType;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.fhir.TestFhirFeedHelper;
import org.openmrs.module.fhir.mapper.model.FHIRIdentifier;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class TestOrderMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    TestOrderMapper testOrderMapper;

    @Autowired
    EncounterService encounterService;

    @Autowired
    ProviderService providerService;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
    }

    @Test
    public void shouldMapTestOrderForAPanelOrTest() throws Exception {
        Encounter encounter = encounterService.getEncounter(36);
        org.hl7.fhir.instance.model.Encounter fhirEncounter = new org.hl7.fhir.instance.model.Encounter();
        assertEquals(1, encounter.getOrders().size());
        AtomFeed feed = new AtomFeed();
        Order order = encounter.getOrders().iterator().next();
        List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, feed, getSystemProperties("1"));
        assertNotNull(mappedResources);
        assertEquals(2, mappedResources.size());
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder)TestFhirFeedHelper.getResourceByType(ResourceType.DiagnosticOrder, mappedResources).getResource();
        assertNotNull(diagnosticOrder);
        assertNotNull(TestFhirFeedHelper.getResourceByType(ResourceType.Specimen, mappedResources));
        assertTrue(diagnosticOrder.getOrderer().getReferenceSimple().endsWith("321.json"));
    }

    @Test
    public void shouldMapTestOrderWithoutLoincName() throws Exception {
        Encounter encounter = encounterService.getEncounter(38);
        org.hl7.fhir.instance.model.Encounter fhirEncounter = new org.hl7.fhir.instance.model.Encounter();
        assertEquals(2, encounter.getOrders().size());
        AtomFeed feed = new AtomFeed();
        Order order = encounter.getOrders().iterator().next();
        List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, feed, getSystemProperties("1"));
        assertNotNull(mappedResources);
        assertEquals(1, mappedResources.size());
    }

    @Test
    public void shouldMapTestOrderForATestAndPanelToSameDiagnosticOrder() throws Exception {
        Encounter encounter = encounterService.getEncounter(38);
        org.hl7.fhir.instance.model.Encounter fhirEncounter = new org.hl7.fhir.instance.model.Encounter();
        assertEquals(2, encounter.getOrders().size());
        AtomFeed feed = new AtomFeed();
        for (Order order : encounter.getOrders()) {
            List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, feed, getSystemProperties("1"));
            assertNotNull(mappedResources);
            addToAtomFeed(feed, mappedResources);
        }
        assertEquals(1, feed.getEntryList().size());
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) feed.getEntryList().get(0).getResource();
        assertEquals(2, diagnosticOrder.getItem().size());
    }

    @Test
    public void shouldNotAddSpecimenIfAlreadyPresentForSameAccession() throws Exception {
        Encounter encounter = encounterService.getEncounter(39);
        org.hl7.fhir.instance.model.Encounter fhirEncounter = new org.hl7.fhir.instance.model.Encounter();
        assertEquals(2, encounter.getOrders().size());
        AtomFeed feed = new AtomFeed();
        for (Order order : encounter.getOrders()) {
            List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, feed, getSystemProperties("1"));
            assertNotNull(mappedResources);
            addToAtomFeed(feed, mappedResources);
        }
        assertEquals(2, feed.getEntryList().size());
    }

    @Test
    public void shouldAddSpecimenIfAlreadyPresentForSameAccession() throws Exception {
        Encounter encounter = encounterService.getEncounter(40);
        org.hl7.fhir.instance.model.Encounter fhirEncounter = new org.hl7.fhir.instance.model.Encounter();
        assertEquals(2, encounter.getOrders().size());
        AtomFeed feed = new AtomFeed();
        for (Order order : encounter.getOrders()) {
            List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, feed, getSystemProperties("1"));
            assertNotNull(mappedResources);
            addToAtomFeed(feed, mappedResources);
        }
        assertEquals(3, feed.getEntryList().size());
    }

    @SuppressWarnings("unchecked")
    private void addToAtomFeed(AtomFeed feed, List<FHIRResource> mappedResources) {
        for (FHIRResource resource : mappedResources) {
            AtomEntry resourceEntry = new AtomEntry();
            resourceEntry.setId(new FHIRIdentifier(resource.getIdentifier().getValueSimple()).getExternalForm());
            resourceEntry.setTitle(resource.getResourceName());
            resourceEntry.setResource(resource.getResource());
            feed.addEntry(resourceEntry);
        }
    }
}