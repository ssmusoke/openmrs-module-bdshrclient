package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.fhir.TestHelper;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class FHIRDiagnosticOrderMapperIT extends BaseModuleWebContextSensitiveTest {
    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private FHIRDiagnosticOrderMapper diagnosticOrderMapper;

    @Autowired
    private PatientService patientService;

    @Autowired
    private EncounterService encounterService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private OrderService orderService;

    private AtomFeed bundle;

    public ParserBase.ResourceOrFeed loadSampleFHIREncounter() throws Exception {
        ParserBase.ResourceOrFeed parsedResource = new TestHelper().loadSampleFHIREncounter("classpath:encounterBundles/encounterWithDiagnosticOrder.xml", springContext);
        return parsedResource;
    }

    @Before
    public void setUp() throws Exception {
        executeDataSet("labOrder.xml");
        bundle = loadSampleFHIREncounter().getFeed();
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
        Resource resource = FHIRFeedHelper.identifyResource(bundle.getEntryList(), ResourceType.DiagnosticOrder);
        Encounter encounter = new Encounter();
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        encounter.setEncounterDatetime(new Date());
        diagnosticOrderMapper.map(bundle, resource, patientService.getPatient(1), encounter, new HashMap<String, List<String>>());
        return encounter;
    }
}