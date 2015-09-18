package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
<<<<<<< HEAD
import ca.uhn.fhir.model.dstu2.resource.MedicationPrescription;
import org.junit.After;
import org.junit.Before;
=======
>>>>>>> Neha | bdshr-916 | refactoring. Fixed few mappers and tests.
import org.junit.Test;
import org.openmrs.api.ConceptService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRMedicationOrderMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private FHIRMedicationOrderMapper mapper;

    @Autowired
    private ConceptService conceptService;
    
    private IResource resource;
    private Bundle bundle;

//    @Before
//    public void setUp() throws Exception {
//        executeDataSet("testDataSets/drugOrderDS.xml");
//
//        bundle = (Bundle) new MapperTestHelper().loadSampleFHIREncounter("encounterBundles/encounterWithMedicationPrescription.xml", springContext);
//        resource = FHIRFeedHelper.identifyResource(bundle.getEntry(), new MedicationPrescription().getResourceName());
//    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }


    @Test
    public void shouldHandleResourceOfTypeMedicationPrescription() throws Exception {
//        assertTrue(mapper.canHandle(resource));
    }

//    @Test
//    public void shouldMapMedicationToDrug(){
//        Order order = getOrder();
//        assertTrue(order instanceof DrugOrder);
//        DrugOrder drugOrder = (DrugOrder) order;
//
//        assertEquals(conceptService.getDrug(301), drugOrder.getDrug());
//        assertEquals(conceptService.getConcept(301), drugOrder.getConcept());
//    }
//
//    @Test
//    public void shouldMapProviderToDrug(){
//        Order order = getOrder();
//        //TODO : test against medication prescription prescriber field.
//        assertNotNull(order.getOrderer());
//    }
//
//    @Test
//    public void shouldSetCareSettingAndNumRefills() throws Exception {
//        Order order = getOrder();
//        assertEquals("OutPatient", order.getCareSetting().getName());
//        assertEquals(0, ((DrugOrder) order).getNumRefills().intValue());
//    }
//
//    @Test
//    public void shouldMapQuantity() throws Exception {
//        DrugOrder order = (DrugOrder) getOrder();
//        assertThat(order.getQuantity().doubleValue(), is(6.0));
//        assertEquals(conceptService.getConcept(810), order.getQuantityUnits());
//    }
//
//    @Test
//    public void shouldMapPatientToDrug(){
//        Encounter mappedEncounter = new Encounter();
//        Patient patient = new Patient();
//        mapper.map(bundle, resource, patient, mappedEncounter,new HashMap<String, List<String>>());
//
//        assertEquals(1, mappedEncounter.getOrders().size());
//        Order order = mappedEncounter.getOrders().iterator().next();
//        assertTrue(order instanceof DrugOrder);
//        DrugOrder drugOrder = (DrugOrder) order;
//
//        assertEquals(patient, drugOrder.getPatient());
//    }
//
//    @Test
//    public void shouldMapFrequencyAndDurationAndScheduledDateToDrug(){
//        Order order = getOrder();
//        assertTrue(order instanceof DrugOrder);
//        DrugOrder drugOrder = (DrugOrder) order;
//
//        assertEquals(2, drugOrder.getDuration().intValue());
//        assertEquals(conceptService.getConcept(803), drugOrder.getDurationUnits());
//
//        assertEquals(conceptService.getConcept(902), drugOrder.getFrequency().getConcept());
//
//        assertEquals(DateUtil.parseDate("2014-12-25T12:21:10+05:30"), drugOrder.getScheduledDate());
//        assertEquals(Order.Urgency.ON_SCHEDULED_DATE, drugOrder.getUrgency());
//    }
//
//    @Test
//    public void shouldMapDrugDosageAndRoutes(){
//        Order order = getOrder();
//        assertTrue(order instanceof DrugOrder);
//        DrugOrder drugOrder = (DrugOrder) order;
//
//        assertEquals(new Double(3), drugOrder.getDose());
//        assertEquals(conceptService.getConcept(806), drugOrder.getDoseUnits());
//        assertEquals(conceptService.getConcept(701), drugOrder.getRoute());
//    }
//
//    private Order getOrder() {
//        Encounter mappedEncounter = new Encounter();
//        Patient patient = new Patient();
//        mapper.map(bundle, resource, patient, mappedEncounter,new HashMap<String, List<String>>());
//
//        assertEquals(1, mappedEncounter.getOrders().size());
//        return mappedEncounter.getOrders().iterator().next();
//    }
}