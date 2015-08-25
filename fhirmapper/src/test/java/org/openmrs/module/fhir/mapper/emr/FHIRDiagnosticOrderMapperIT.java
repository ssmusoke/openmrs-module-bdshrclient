package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRDiagnosticOrderMapperIT extends BaseModuleWebContextSensitiveTest {
    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private FHIRDiagnosticOrderMapper diagnosticOrderMapper;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private OrderService orderService;

    private Bundle bundle;

    public Bundle loadSampleFHIREncounter() throws Exception {
        return (Bundle) new MapperTestHelper().loadSampleFHIREncounter("classpath:encounterBundles/dstu2/encounterWithDiagnosticOrder.xml", springContext);
    }

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        bundle = loadSampleFHIREncounter();
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }


    @Test
    public void shouldMapDiagnosticOrder() throws Exception {
        Encounter encounter = mapOrder();
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        Order order = orders.iterator().next();
        assertEquals("7f7379ba-3ca8-11e3-bf2b-0800271c1b75", order.getConcept().getUuid());
        assertEquals(providerService.getProvider(22), order.getOrderer());
        assertEquals(orderService.getOrderType(16), order.getOrderType());
        assertEquals(orderService.getCareSetting(1), order.getCareSetting());
        assertNotNull(order.getDateActivated());
    }

    private Encounter mapOrder() {
        IResource resource = FHIRFeedHelper.identifyResource(bundle.getEntry(), new DiagnosticOrder().getResourceName());
        Encounter encounter = new Encounter();
        encounter.setEncounterDatetime(new Date());
        diagnosticOrderMapper.map(bundle, resource, patientService.getPatient(1), encounter, new HashMap<String, List<String>>());
        return encounter;
    }
}