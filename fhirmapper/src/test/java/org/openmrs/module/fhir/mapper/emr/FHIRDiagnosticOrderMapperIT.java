package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.mapper.model.EmrEncounter;
import org.openmrs.module.fhir.mapper.model.ShrEncounterBundle;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.fhir.utils.FHIRBundleHelper;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.openmrs.Order.Action.DISCONTINUE;
import static org.openmrs.Order.Action.NEW;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRDiagnosticOrderMapperIT extends BaseModuleWebContextSensitiveTest {
    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private FHIRDiagnosticOrderMapper diagnosticOrderMapper;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private EncounterService encounterService;

    @Autowired
    private IdMappingRepository idMappingRepository;

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldMapDiagnosticOrder() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithDiagnosticOrderWithItemEventDate.xml", "HIDA764177", "shr-enc-id");
        Set<Order> orders = emrEncounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        Order order = orders.iterator().next();
        assertEquals("7f7379ba-3ca8-11e3-bf2b-0800271c1b75", order.getConcept().getUuid());
        assertEquals(providerService.getProvider(23), order.getOrderer());
        assertEquals(orderService.getOrderType(16), order.getOrderType());
        assertEquals(orderService.getCareSetting(1), order.getCareSetting());
        assertEquals(DateUtil.parseDate("2015-08-24T17:10:10.000+05:30"), order.getDateActivated());
        assertEquals(DateUtil.parseDate("2015-08-25T17:10:10.000+05:30"), order.getAutoExpireDate());
        IdMapping idMapping = idMappingRepository.findByExternalId("shr-enc-id:453b7b24-7847-49f7-8a33-2fc339e5c4c7", IdMappingType.DIAGNOSTIC_ORDER);
        assertEquals(order.getUuid(), idMapping.getInternalId());
        assertEquals(IdMappingType.DIAGNOSTIC_ORDER, idMapping.getType());
        assertEquals("http://shr.com/patients/HIDA764177/encounters/shr-enc-id#DiagnosticOrder/453b7b24-7847-49f7-8a33-2fc339e5c4c7",
                idMapping.getUri());
    }

    @Test
    public void shouldMapDiagnosticOrderWithoutOrderer() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        int shrClientSystemProviderId = 22;
        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithDiagnosticOrderWithoutOrderer.xml", "HIDA764177", "shr-enc-id-1");
        Set<Order> orders = emrEncounter.getOrders();
        assertEquals(1, orders.size());
        Order order = orders.iterator().next();
        assertThat(order.getOrderer().getProviderId(), is(shrClientSystemProviderId));
    }

    @Test
    public void shouldUpdateSameEncounterIfNewOrdersAreAdded() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        Encounter existingEncounter = encounterService.getEncounter(42);
        Set<Order> orders = existingEncounter.getOrders();
        assertEquals(1, orders.size());
        Order existingOrder = orders.iterator().next();
        assertNull(existingOrder.getDateStopped());

        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithUpdatedDiagnosticOrder.xml", existingEncounter, "HIDA764177", "shr-enc-id-1");

        Set<Order> emrEncounterOrders = emrEncounter.getOrders();
        assertEquals(1, emrEncounterOrders.size());
        Order newOrder = emrEncounterOrders.iterator().next();
        assertEquals(NEW, newOrder.getAction());
        assertThat(newOrder.getConcept().getId(), is(303));
    }

    @Test
    public void shouldDiscontinueAnExistingOrderIfUpdatedAsCancelled() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        Encounter existingEncounter = encounterService.getEncounter(42);
        Set<Order> orders = existingEncounter.getOrders();
        assertEquals(1, orders.size());
        Order existingOrder = orders.iterator().next();
        assertNull(existingOrder.getDateStopped());

        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithCanceledDiagnosticOrder.xml", existingEncounter, "HIDA764177", "shr-enc-id-1");

        Set<Order> emrEncounterOrders = emrEncounter.getOrders();
        assertEquals(1, emrEncounterOrders.size());
        Order discontinuedOrder = emrEncounterOrders.iterator().next();
        assertEquals(DISCONTINUE, discontinuedOrder.getAction());
        assertEquals(existingOrder, discontinuedOrder.getPreviousOrder());
        assertEquals(DateUtil.parseDate("2015-08-25T18:10:10.000+05:30"), discontinuedOrder.getDateActivated());
    }

    @Test
    public void shouldNotDoAnyThingIfOrderWasNotDownloadedAndUpdatedAsCancelled() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithCanceledDiagnosticOrder.xml", "HIDA764177", "shr-enc-id-2");
        Set<Order> emrEncounterOrders = emrEncounter.getOrders();
        assertTrue(CollectionUtils.isEmpty(emrEncounterOrders));
    }

    @Test
    public void shouldProcessAllCancelledOrdersFirst() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        Encounter existingEncounter = encounterService.getEncounter(42);
        Set<Order> orders = existingEncounter.getOrders();
        assertEquals(1, orders.size());
        Order existingOrder = orders.iterator().next();
        assertNull(existingOrder.getDateStopped());

        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithDiagnosticOrderCancelledAndRequested.xml", existingEncounter, "HIDA764177", "shr-enc-id-1");

        Set<Order> emrEncounterOrders = emrEncounter.getOrders();
        assertEquals(2, emrEncounterOrders.size());
        Order discontinuedOrder = getOrderWithAction(emrEncounterOrders, DISCONTINUE);
        assertNotNull(discontinuedOrder);
        assertEquals(existingOrder, discontinuedOrder.getPreviousOrder());
        assertThat(discontinuedOrder.getConcept().getId(), is(304));

        Order newOrder = getOrderWithAction(emrEncounterOrders, NEW);
        assertNotNull(newOrder);
        assertThat(newOrder.getConcept().getId(), is(304));
    }

    @Test
    public void shouldSetOrderDateActivatedFromDiagnosticOrderEventIfNotPresentInItem() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/diagnositicOrderWithEventDateAndCategory.xml", "HIDA764177", "shr-enc-id-1");
        Set<Order> orders = emrEncounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        Order order = orders.iterator().next();
        assertEquals("7f7379ba-3ca8-11e3-bf2b-0800271c1b75", order.getConcept().getUuid());
        assertEquals(DateUtil.parseDate("2015-08-24T18:10:10.000+05:30"), order.getDateActivated());
        assertNotNull(order.getAutoExpireDate());
    }

    @Test
    public void shouldMapLabOrderFromCategory() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/diagnositicOrderWithEventDateAndCategory.xml", "HIDA764177", "shr-enc-id-1");
        Set<Order> orders = emrEncounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        Order order = orders.iterator().next();
        assertEquals(MRSProperties.MRS_LAB_ORDER_TYPE, order.getOrderType().getName());
    }

    @Test
    public void shouldSetOrderDateActivatedFromEncounterIfEventNotPresent() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        Encounter encounter = new Encounter();
        Date encounterDatetime = new Date();
        encounter.setEncounterDatetime(encounterDatetime);
        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithDiagnosticOrderWithoutEventDate.xml", encounter, "HIDA764177", "shr-enc-id-1");
        Set<Order> orders = emrEncounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        Order order = orders.iterator().next();
        assertEquals("7f7379ba-3ca8-11e3-bf2b-0800271c1b75", order.getConcept().getUuid());
        assertEquals(encounterDatetime, order.getDateActivated());
        assertNotNull(order.getAutoExpireDate());
    }

    @Test
    public void shouldMapRadiologyOrders() throws Exception {
        executeDataSet("testDataSets/radiologyOrderDS.xml");
        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithRadiologyOrder.xml", "HIDA764177", "shr-enc-id");
        Set<Order> orders = emrEncounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        Order order = orders.iterator().next();
        assertEquals("7f7379ba-3ca8-11e3-bf2b-0800271c1b75", order.getConcept().getUuid());
        assertEquals(providerService.getProvider(23), order.getOrderer());
        assertEquals(orderService.getOrderType(16), order.getOrderType());
        assertEquals(orderService.getCareSetting(1), order.getCareSetting());
        assertEquals(DateUtil.parseDate("2016-03-11T13:02:16.000+05:30"), order.getDateActivated());
        assertEquals(DateUtil.parseDate("2016-03-12T13:02:16.000+05:30"), order.getAutoExpireDate());
        IdMapping idMapping = idMappingRepository.findByExternalId("shr-enc-id:bdee83c1-f559-433f-8932-8711f6174676", IdMappingType.DIAGNOSTIC_ORDER);
        assertEquals(order.getUuid(), idMapping.getInternalId());
        assertEquals(IdMappingType.DIAGNOSTIC_ORDER, idMapping.getType());
        assertEquals("http://shr.com/patients/HIDA764177/encounters/shr-enc-id#DiagnosticOrder/bdee83c1-f559-433f-8932-8711f6174676",
                idMapping.getUri());
    }

    @Test
    public void shouldSkipDownloadOfOrderIfCategoryCodeNotPresentLocalMaps() throws Exception {
        executeDataSet("testDataSets/radiologyOrderDS.xml");
        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithOrderCategoryCodeNotPresentLocally.xml", "HIDA764177", "shr-enc-id");
        assertTrue(CollectionUtils.isEmpty(emrEncounter.getOrders()));
    }

    private Order getOrderWithAction(Set<Order> orders, Order.Action action) {
        for (Order order : orders) {
            if (order.getAction().equals(action)) return order;
        }
        return null;
    }

    private EmrEncounter mapOrder(String filePath, String healthId, String shrEncounterId) throws Exception {
        Encounter encounter = new Encounter();
        encounter.setEncounterDatetime(new Date());
        return mapOrder(filePath, encounter, healthId, shrEncounterId);
    }

    private EmrEncounter mapOrder(String filePath, Encounter encounter, String healthId, String shrEncounterId) throws Exception {
        Bundle bundle = loadSampleFHIREncounter(filePath);
        IResource resource = FHIRBundleHelper.identifyFirstResourceWithName(bundle, new DiagnosticOrder().getResourceName());
        ShrEncounterBundle encounterComposition = new ShrEncounterBundle(bundle, healthId, shrEncounterId);
        EmrEncounter emrEncounter = new EmrEncounter(encounter);
        diagnosticOrderMapper.map(resource, emrEncounter, encounterComposition, getSystemProperties("1"));
        return emrEncounter;
    }

    private Bundle loadSampleFHIREncounter(String filePath) throws Exception {
        return (Bundle) new MapperTestHelper().loadSampleFHIREncounter(filePath, springContext);
    }
}