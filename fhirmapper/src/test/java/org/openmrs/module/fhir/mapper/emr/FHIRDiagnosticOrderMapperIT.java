package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.mapper.model.EmrEncounter;
import org.openmrs.module.fhir.mapper.model.ShrEncounter;
import org.openmrs.module.fhir.utils.FHIRBundleHelper;
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

    private Bundle loadSampleFHIREncounter(String filePath) throws Exception {
        return (Bundle) new MapperTestHelper().loadSampleFHIREncounter(filePath, springContext);
    }

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldMapDiagnosticOrder() throws Exception {
        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithDiagnosticOrder.xml");
        Set<Order> orders = emrEncounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        Order order = orders.iterator().next();
        assertEquals("7f7379ba-3ca8-11e3-bf2b-0800271c1b75", order.getConcept().getUuid());
        assertEquals(providerService.getProvider(23), order.getOrderer());
        assertEquals(orderService.getOrderType(16), order.getOrderType());
        assertEquals(orderService.getCareSetting(1), order.getCareSetting());
        assertNotNull(order.getDateActivated());
    }

    @Test
    public void shouldMapDiagnosticOrderWithoutOrderer() throws Exception {
        int shrClientSystemProviderId = 22;
        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithDiagnosticOrderWithoutOrderer.xml");
        Set<Order> orders = emrEncounter.getOrders();
        assertEquals(1, orders.size());
        Order order = orders.iterator().next();
        assertThat(order.getOrderer().getProviderId(), is(shrClientSystemProviderId));
    }

    @Test
    public void shouldUpdateSameEncounterIfNewOrdersAreAdded() throws Exception {
        Encounter existingEncounter = encounterService.getEncounter(42);
        Set<Order> orders = existingEncounter.getOrders();
        assertEquals(1, orders.size());
        Order existingOrder = orders.iterator().next();
        assertNull(existingOrder.getDateStopped());

        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithUpdatedDiagnosticOrder.xml", existingEncounter);

        Set<Order> emrEncounterOrders = emrEncounter.getOrders();
        assertEquals(1, emrEncounterOrders.size());
        Order newOrder = emrEncounterOrders.iterator().next();
        assertEquals(NEW, newOrder.getAction());
        assertThat(newOrder.getConcept().getId(), is(303));
    }

    @Test
    public void shouldDiscontinueAnExistingOrderIfUpdatedAsCancelled() throws Exception {
        Encounter existingEncounter = encounterService.getEncounter(42);
        Set<Order> orders = existingEncounter.getOrders();
        assertEquals(1, orders.size());
        Order existingOrder = orders.iterator().next();
        assertNull(existingOrder.getDateStopped());

        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithCanceledDiagnosticOrder.xml", existingEncounter);

        Set<Order> emrEncounterOrders = emrEncounter.getOrders();
        assertEquals(1, emrEncounterOrders.size());
        Order discontinuedOrder = emrEncounterOrders.iterator().next();
        assertEquals(DISCONTINUE, discontinuedOrder.getAction());
        assertEquals(existingOrder, discontinuedOrder.getPreviousOrder());
    }

    @Test
    public void shouldNotDoAnyThingIfOrderWasNotDownloadedAndUpdatedAsCancelled() throws Exception {
        EmrEncounter emrEncounter = mapOrder("encounterBundles/dstu2/encounterWithCanceledDiagnosticOrder.xml");
        Set<Order> emrEncounterOrders = emrEncounter.getOrders();
        assertTrue(CollectionUtils.isEmpty(emrEncounterOrders));
    }

    private EmrEncounter mapOrder(String filePath) throws Exception {
        Encounter encounter = new Encounter();
        encounter.setEncounterDatetime(new Date());
        return mapOrder(filePath, encounter);
    }

    private EmrEncounter mapOrder(String filePath, Encounter encounter) throws Exception {
        Bundle bundle = loadSampleFHIREncounter(filePath);
        IResource resource = FHIRBundleHelper.identifyResource(bundle.getEntry(), new DiagnosticOrder().getResourceName());
        ShrEncounter encounterComposition = new ShrEncounter(bundle, "HIDA764177", "shr-enc-id-1");
        EmrEncounter emrEncounter = new EmrEncounter(encounter);
        diagnosticOrderMapper.map(resource, emrEncounter, encounterComposition, getSystemProperties("1"));
        return emrEncounter;
    }
}