package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import ca.uhn.fhir.model.dstu2.resource.Specimen;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticOrderStatusEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.TestFhirFeedHelper;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TestOrderMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private TestOrderMapper testOrderMapper;

    @Autowired
    private EncounterService encounterService;

    @Autowired
    private OrderService orderService;
    private final String patientRef = "http://mci.com/patients/HID-123";
    private final String fhirEncounterId = "SHR-ENC1";

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldMapATestOrder() throws Exception {
        Order order = orderService.getOrder(17);
        List<FHIRResource> mappedResources = testOrderMapper.map(order, createFhirEncounter(), new Bundle(), getSystemProperties("1"));
        assertTrue(CollectionUtils.isNotEmpty(mappedResources));
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) mappedResources.get(0).getResource();
        assertDiagnosticOrder(patientRef, fhirEncounterId, diagnosticOrder);
        assertOrderItems(diagnosticOrder);
    }

    @Test
    public void shouldMapTestOrderForAPanelOrTest() throws Exception {
        Encounter encounter = encounterService.getEncounter(36);
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = createFhirEncounter();
        assertEquals(1, encounter.getOrders().size());
        Bundle bundle = new Bundle();
        Order order = encounter.getOrders().iterator().next();
        List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, bundle, getSystemProperties("1"));
        assertNotNull(mappedResources);
        assertEquals(2, mappedResources.size());
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) TestFhirFeedHelper.getFirstResourceByType(new DiagnosticOrder().getResourceName(), mappedResources).getResource();
        assertNotNull(diagnosticOrder);
        assertNotNull(TestFhirFeedHelper.getFirstResourceByType(new Specimen().getResourceName(), mappedResources));
        assertTrue(diagnosticOrder.getOrderer().getReference().getValue().endsWith("812.json"));
    }


    @Test
    public void shouldMapTestOrderWithoutLoincName() throws Exception {
        Encounter encounter = encounterService.getEncounter(38);
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = createFhirEncounter();
        assertEquals(2, encounter.getOrders().size());
        Bundle bundle = new Bundle();
        Order order = encounter.getOrders().iterator().next();
        List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, bundle, getSystemProperties("1"));
        assertNotNull(mappedResources);
        assertEquals(1, mappedResources.size());
    }

    @Test
    public void shouldMapTestOrdersToSameDiagnosticOrder() throws Exception {
        Encounter encounter = encounterService.getEncounter(38);
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = createFhirEncounter();
        assertEquals(2, encounter.getOrders().size());
        Bundle bundle = new Bundle();
        Iterator<Order> orderIterator = encounter.getOrders().iterator();
        Order firstOrder = orderIterator.next();
        SystemProperties systemProperties = getSystemProperties("1");

        List<FHIRResource> mappedResources = testOrderMapper.map(firstOrder, fhirEncounter, bundle, systemProperties);
        assertEquals(1, mappedResources.size());
        IResource diagnosticOrderResource = mappedResources.get(0).getResource();
        assertTrue(diagnosticOrderResource instanceof DiagnosticOrder);
        addToAtomFeed(bundle, mappedResources);
        assertEquals(1, ((DiagnosticOrder) diagnosticOrderResource).getItem().size());

        mappedResources.addAll(testOrderMapper.map(orderIterator.next(), fhirEncounter, bundle, systemProperties));
        assertEquals(1, mappedResources.size());
        assertEquals(2, ((DiagnosticOrder) diagnosticOrderResource).getItem().size());
    }

    @Test
    public void shouldAddASpecimen() throws Exception {
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = createFhirEncounter();
        Order order = orderService.getOrder(20);
        List<FHIRResource> mappedResources = testOrderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        assertEquals(2, mappedResources.size());

        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) TestFhirFeedHelper.getFirstResourceByType(new DiagnosticOrder().getResourceName(), mappedResources).getResource();
        assertEquals(1, diagnosticOrder.getSpecimen().size());
        List<DiagnosticOrder.Item> items = diagnosticOrder.getItem();
        assertEquals(1, items.size());

        FHIRResource resourceByType = TestFhirFeedHelper.getFirstResourceByType(new Specimen().getResourceName(), mappedResources);
        DiagnosticOrder.Item item = items.get(0);
        Specimen specimen = (Specimen) resourceByType.getResource();
        assertSpecimen(specimen, "urn:uuid:accession-number", "Bld");
        assertEquals(diagnosticOrder.getSpecimen().get(0).getReference().getValue(), specimen.getIdentifier().get(0).getValue());
        assertEquals(item.getSpecimen().get(0).getReference().getValue(), specimen.getIdentifier().get(0).getValue());
    }

    @Test
    public void shouldNotAddSpecimenIfAlreadyPresentForSameAccession() throws Exception {
        Encounter encounter = encounterService.getEncounter(39);
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = createFhirEncounter();
        assertEquals(2, encounter.getOrders().size());
        Bundle bundle = new Bundle();
        Iterator<Order> orderIterator = encounter.getOrders().iterator();

        List<FHIRResource> mappedResources = testOrderMapper.map(orderIterator.next(), fhirEncounter, bundle, getSystemProperties("1"));
        assertEquals(2, mappedResources.size());
        ArrayList<FHIRResource> diagnosticOrderResource = TestFhirFeedHelper.getResourceByType(new DiagnosticOrder().getResourceName(), mappedResources);
        assertEquals(1, diagnosticOrderResource.size());
        List<FHIRResource> specimenResources = TestFhirFeedHelper.getResourceByType(new Specimen().getResourceName(), mappedResources);
        assertEquals(1, specimenResources.size());
        assertSpecimen((Specimen) specimenResources.get(0).getResource(), "urn:uuid:accession-number", "Bld");
        addToAtomFeed(bundle, mappedResources);

        List<FHIRResource> newMappedResources = testOrderMapper.map(orderIterator.next(), fhirEncounter, bundle, getSystemProperties("1"));
        assertEquals(0, newMappedResources.size());
    }

    @Test
    public void shouldAddSpecimenForDifferentAccession() throws Exception {
        Encounter encounter = encounterService.getEncounter(40);
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = createFhirEncounter();
        assertEquals(2, encounter.getOrders().size());
        Bundle bundle = new Bundle();
        Iterator<Order> orderIterator = encounter.getOrders().iterator();

        List<FHIRResource> mappedResources = testOrderMapper.map(orderIterator.next(), fhirEncounter, bundle, getSystemProperties("1"));
        assertEquals(2, mappedResources.size());
        ArrayList<FHIRResource> diagnosticOrderResource = TestFhirFeedHelper.getResourceByType(new DiagnosticOrder().getResourceName(), mappedResources);
        assertEquals(1, diagnosticOrderResource.size());
        List<FHIRResource> specimenResources = TestFhirFeedHelper.getResourceByType(new Specimen().getResourceName(), mappedResources);
        assertEquals(1, specimenResources.size());
        assertSpecimen((Specimen) specimenResources.get(0).getResource(), "urn:uuid:accession-number1", "Bld");
        addToAtomFeed(bundle, mappedResources);

        List<FHIRResource> newMappedResources = testOrderMapper.map(orderIterator.next(), fhirEncounter, bundle, getSystemProperties("1"));
        assertEquals(1, newMappedResources.size());
        ArrayList<FHIRResource> newSpecimenResources = TestFhirFeedHelper.getResourceByType(new Specimen().getResourceName(), newMappedResources);
        assertEquals(1, newSpecimenResources.size());
        assertSpecimen((Specimen) newSpecimenResources.get(0).getResource(), "urn:uuid:accession-number2", "Bld");
    }

    @Test
    public void shouldNotMapAStoppedTestOrder() throws Exception {
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = createFhirEncounter();
        Order order = orderService.getOrder(24);
        List<FHIRResource> fhirResources = testOrderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        assertTrue(fhirResources.isEmpty());
    }

    @Test
    public void shouldMapADiscountinuedOrder() throws Exception {
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = createFhirEncounter();
        Order order = orderService.getOrder(25);
        List<FHIRResource> fhirResources = testOrderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        assertEquals(1, fhirResources.size());
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) fhirResources.get(0).getResource();
        List<DiagnosticOrder.Item> items = diagnosticOrder.getItem();
        assertEquals(1, items.size());
        assertEquals(DiagnosticOrderStatusEnum.CANCELLED.getCode(), items.get(0).getStatus());
    }

    private void assertOrderItems(DiagnosticOrder diagnosticOrder) {
        assertEquals(DiagnosticOrderStatusEnum.REQUESTED.getCode(), diagnosticOrder.getStatus());
        assertEquals(1, diagnosticOrder.getItem().size());
        assertTrue(MapperTestHelper.containsCoding(diagnosticOrder.getItemFirstRep().getCode().getCoding(), null, null, "Urea Nitorgen"));
    }

    private void assertSpecimen(Specimen specimen, String accessionNo, String specimenType) {
        assertEquals(patientRef, specimen.getSubject().getReference().getValue());
        assertEquals(accessionNo, specimen.getAccessionIdentifier().getValue());
        assertFalse(specimen.getId().isEmpty());
        assertTrue(CollectionUtils.isNotEmpty(specimen.getIdentifier()));
        assertFalse(specimen.getIdentifier().get(0).isEmpty());
        assertTrue(MapperTestHelper.containsCoding(specimen.getType().getCoding(), null, null, specimenType));
    }

    private void assertDiagnosticOrder(String patientRef, String fhirEncounterId, DiagnosticOrder diagnosticOrder) {
        assertEquals(patientRef, diagnosticOrder.getSubject().getReference().getValue());
        assertTrue(diagnosticOrder.getOrderer().getReference().getValue().endsWith("812.json"));
        assertFalse(diagnosticOrder.getId().isEmpty());
        assertTrue(CollectionUtils.isNotEmpty(diagnosticOrder.getIdentifier()));
        assertFalse(diagnosticOrder.getIdentifier().get(0).isEmpty());
        assertEquals(fhirEncounterId, diagnosticOrder.getEncounter().getReference().getValue());
        assertEquals(DiagnosticOrderStatusEnum.REQUESTED.getCode(), diagnosticOrder.getStatus());
    }

    private ca.uhn.fhir.model.dstu2.resource.Encounter createFhirEncounter() {
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = new ca.uhn.fhir.model.dstu2.resource.Encounter();
        fhirEncounter.setPatient(new ResourceReferenceDt(patientRef));
        fhirEncounter.setId(fhirEncounterId);
        return fhirEncounter;
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