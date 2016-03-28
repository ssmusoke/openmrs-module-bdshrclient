package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticOrderStatusEnum;
import ca.uhn.fhir.model.primitive.StringDt;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.FHIRProperties;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.TestFhirFeedHelper;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.module.fhir.FHIRProperties.DIAGNOSTIC_ORDER_CATEGORY_EXTENSION_NAME;
import static org.openmrs.module.fhir.FHIRProperties.FHIR_DIAGNOSTIC_REPORT_CATEGORY_RADIOLOGY_CODE;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GenericOrderMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private GenericOrderMapper genericOrderMapper;

    @Autowired
    private EncounterService encounterService;

    @Autowired
    private OrderService orderService;

    private final String patientRef = "http://mci.com/patients/HID-123";
    private final String fhirEncounterId = "SHR-ENC1";

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldHandleRadiologyOrder() throws Exception {
        executeDataSet("testDataSets/radiologyOrderDS.xml");
        Order order = orderService.getOrder(16);
        assertTrue(genericOrderMapper.canHandle(order));
    }

    @Test
    public void shouldNotHandleTestOrder() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        Order order = orderService.getOrder(20);
        assertFalse(genericOrderMapper.canHandle(order));
    }

    @Test
    public void shouldNotHandleDrugOrder() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        Order order = orderService.getOrder(77);
        assertFalse(genericOrderMapper.canHandle(order));
    }

    @Test
    public void shouldMapALocalRadiologyOrder() throws Exception {
        executeDataSet("testDataSets/radiologyOrderDS.xml");
        Order order = orderService.getOrder(16);
        List<FHIRResource> mappedResources = genericOrderMapper.map(order, createFhirEncounter(), new Bundle(), getSystemProperties("1"));
        assertTrue(CollectionUtils.isNotEmpty(mappedResources));
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) mappedResources.get(0).getResource();
        assertDiagnosticOrder(diagnosticOrder, order.getUuid());
        assertEquals(1, diagnosticOrder.getItem().size());
        DiagnosticOrder.Item item = diagnosticOrder.getItemFirstRep();
        assertTrue(MapperTestHelper.containsCoding(item.getCode().getCoding(), null, null, "X-ray left hand"));
        assertEquals(1, item.getEvent().size());
        assertTrue(hasEventWithDateTime(item, DiagnosticOrderStatusEnum.REQUESTED, order.getDateActivated()));
    }

    @Test
    public void shouldMapTRRadiologyOrder() throws Exception {
        executeDataSet("testDataSets/radiologyOrderDS.xml");
        Encounter encounter = encounterService.getEncounter(37);
        FHIREncounter fhirEncounter = createFhirEncounter();
        assertEquals(1, encounter.getOrders().size());
        Bundle bundle = new Bundle();
        Order order = encounter.getOrders().iterator().next();
        List<FHIRResource> mappedResources = genericOrderMapper.map(order, fhirEncounter, bundle, getSystemProperties("1"));
        assertNotNull(mappedResources);
        assertEquals(1, mappedResources.size());
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) TestFhirFeedHelper.getFirstResourceByType(new DiagnosticOrder().getResourceName(), mappedResources).getResource();
        assertNotNull(diagnosticOrder);
        assertTrue(diagnosticOrder.getOrderer().getReference().getValue().endsWith("812.json"));
        assertEquals(1, diagnosticOrder.getItem().size());
        assertTrue(MapperTestHelper.containsCoding(diagnosticOrder.getItemFirstRep().getCode().getCoding(), "501qb827-a67c-4q1f-a705-e5efe0q6a972",
                "http://localhost:9997/openmrs/ws/rest/v1/tr/concept/501qb827-a67c-4q1f-a705-e5efe0q6a972", "X-Ray Right Chest"));
    }

    @Test
    public void shouldNotMapAStoppedTestOrder() throws Exception {
        executeDataSet("testDataSets/radiologyOrderDS.xml");
        FHIREncounter fhirEncounter = createFhirEncounter();
        Order order = orderService.getOrder(18);
        List<FHIRResource> fhirResources = genericOrderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        assertTrue(fhirResources.isEmpty());
    }

    @Test
    public void shouldMapADiscountinuedOrder() throws Exception {
        executeDataSet("testDataSets/radiologyOrderDS.xml");
        FHIREncounter fhirEncounter = createFhirEncounter();
        Order order = orderService.getOrder(19);
        List<FHIRResource> fhirResources = genericOrderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        assertEquals(1, fhirResources.size());
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) fhirResources.get(0).getResource();
        assertDiagnosticOrder(diagnosticOrder, order.getPreviousOrder().getUuid());
        List<DiagnosticOrder.Item> items = diagnosticOrder.getItem();
        assertEquals(1, items.size());
        DiagnosticOrder.Item item = items.get(0);
        assertEquals(DiagnosticOrderStatusEnum.CANCELLED.getCode(), item.getStatus());
        assertEquals(2, item.getEvent().size());
        assertTrue(hasEventWithDateTime(item, DiagnosticOrderStatusEnum.CANCELLED, order.getDateActivated()));
        assertTrue(hasEventWithDateTime(item, DiagnosticOrderStatusEnum.REQUESTED, order.getPreviousOrder().getDateActivated()));
    }

    private boolean hasEventWithDateTime(DiagnosticOrder.Item item, DiagnosticOrderStatusEnum status, Date datetime) {
        for (DiagnosticOrder.Event event : item.getEvent()) {
            if (event.getStatus().equals(status.getCode()) && event.getDateTime().equals(datetime)) return true;
        }
        return false;
    }

    private void assertDiagnosticOrder(DiagnosticOrder diagnosticOrder, String orderId) {
        assertEquals(patientRef, diagnosticOrder.getSubject().getReference().getValue());
        assertTrue(diagnosticOrder.getOrderer().getReference().getValue().endsWith("812.json"));
        orderId = "urn:uuid:" + orderId;
        assertEquals(orderId, diagnosticOrder.getId().getValue());
        assertEquals(1, diagnosticOrder.getIdentifier().size());
        assertEquals(orderId, diagnosticOrder.getIdentifierFirstRep().getValue());
        assertFalse(diagnosticOrder.getIdentifier().get(0).isEmpty());
        assertEquals(fhirEncounterId, diagnosticOrder.getEncounter().getReference().getValue());
        assertEquals(1, diagnosticOrder.getUndeclaredExtensions().size());
        ExtensionDt extensionDt = diagnosticOrder.getUndeclaredExtensionsByUrl(FHIRProperties.getFhirExtensionUrl(DIAGNOSTIC_ORDER_CATEGORY_EXTENSION_NAME)).get(0);
        assertTrue(extensionDt.getValue() instanceof StringDt);
        assertEquals(FHIR_DIAGNOSTIC_REPORT_CATEGORY_RADIOLOGY_CODE, ((StringDt)extensionDt.getValue()).getValue());
    }

    private FHIREncounter createFhirEncounter() {
        ca.uhn.fhir.model.dstu2.resource.Encounter encounter = new ca.uhn.fhir.model.dstu2.resource.Encounter();
        encounter.setPatient(new ResourceReferenceDt(patientRef));
        encounter.setId(fhirEncounterId);
        return new FHIREncounter(encounter);
    }
}