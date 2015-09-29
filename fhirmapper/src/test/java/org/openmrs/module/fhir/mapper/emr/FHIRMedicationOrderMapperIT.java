package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRMedicationOrderMapperIT extends BaseModuleWebContextSensitiveTest {

    private MapperTestHelper mapperTestHelper;
    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private FHIRMedicationOrderMapper mapper;

    @Autowired
    private ConceptService conceptService;
    private MedicationOrder medicationOrder;
    private Bundle medicationOrderBundle;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        mapperTestHelper = new MapperTestHelper();

        medicationOrderBundle = (Bundle) mapperTestHelper.loadSampleFHIREncounter("encounterBundles/dstu2/encounterWithMedicationOrder.xml", springContext);
        medicationOrder = (MedicationOrder) FHIRFeedHelper.identifyResource(medicationOrderBundle.getEntry(), new MedicationOrder().getResourceName());
        Thread.sleep(5000);
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }


    @Test
    public void shouldHandleResourceOfTypeMedicationPrescription() throws Exception {
        assertTrue(mapper.canHandle(medicationOrder));
    }

    @Test
    public void shouldMapMedicationToDrug() throws Exception {
        Order order = getOrder(medicationOrderBundle, medicationOrder);
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertEquals(conceptService.getDrug(301), drugOrder.getDrug());
        assertEquals(conceptService.getConcept(301), drugOrder.getConcept());
    }

    @Test
    public void shouldMapProviderToDrug() throws Exception {
        Order order = getOrder(medicationOrderBundle, medicationOrder);
        assertThat(order.getOrderer().getProviderId(), is(22));
    }

    @Test
    public void shouldSetCareSettingAndNumRefills() throws Exception {
        Order order = getOrder(medicationOrderBundle, medicationOrder);
        assertEquals("OutPatient", order.getCareSetting().getName());
        assertEquals(0, ((DrugOrder) order).getNumRefills().intValue());
    }

    @Test
    public void shouldMapPatient() throws Exception {
        Encounter mappedEncounter = new Encounter();
        Patient patient = new Patient();
        mapper.map(medicationOrderBundle, medicationOrder, patient, mappedEncounter);

        assertEquals(1, mappedEncounter.getOrders().size());
        Order order = mappedEncounter.getOrders().iterator().next();
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertEquals(patient, drugOrder.getPatient());
    }

    @Test
    public void shouldMapDurationAndScheduledDateToDrug() throws Exception {
        Bundle bundle = (Bundle) mapperTestHelper.loadSampleFHIREncounter("encounterBundles/dstu2/encounterWithMedicationOrderWithScheduledDate.xml", springContext);
        MedicationOrder resource = (MedicationOrder) FHIRFeedHelper.identifyResource(bundle.getEntry(), new MedicationOrder().getResourceName());

        Order order = getOrder(bundle, resource);
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertEquals(5, drugOrder.getDuration().intValue());
        assertEquals(conceptService.getConcept(803), drugOrder.getDurationUnits());
    }

    @Test
    public void shouldMapDrugRouteAndAsNeeded() throws Exception {
        Order order = getOrder(medicationOrderBundle, medicationOrder);
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertEquals(conceptService.getConcept(701), drugOrder.getRoute());
        assertTrue(drugOrder.getAsNeeded());
    }

    @Test
    public void shouldMapDrugDosageAndFrequency() throws Exception {
        Order order = getOrder(medicationOrderBundle, medicationOrder);
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertThat(drugOrder.getDose(), is(10.0));
        assertEquals(conceptService.getConcept(50), drugOrder.getDoseUnits());

        assertEquals(conceptService.getConcept(903), drugOrder.getFrequency().getConcept());
    }

    private Order getOrder(Bundle bundle, IResource resource) {
        Encounter mappedEncounter = new Encounter();
        Patient patient = new Patient();
        mapper.map(bundle, resource, patient, mappedEncounter);

        assertEquals(1, mappedEncounter.getOrders().size());
        return mappedEncounter.getOrders().iterator().next();
    }
}