package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.bahmniemrapi.drugorder.dosinginstructions.FlexibleDosingInstructions;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
        assertEquals(FlexibleDosingInstructions.class, drugOrder.getDosingType());
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

    @Test
    public void shouldMapDoseFromMedicationFormsValueset() throws Exception {
        Bundle bundle = (Bundle) mapperTestHelper.loadSampleFHIREncounter("encounterBundles/dstu2/encounterWithMedicationOrderWithScheduledDate.xml", springContext);
        MedicationOrder resource = (MedicationOrder) FHIRFeedHelper.identifyResource(bundle.getEntry(), new MedicationOrder().getResourceName());

        Order order = getOrder(bundle, resource);
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertThat(drugOrder.getDose(), is(10.0));
        assertEquals(conceptService.getConcept(807), drugOrder.getDoseUnits());
    }

    @Test
    public void shouldMapQuantityFromDispenseRequest() throws Exception {
        Order order = getOrder(medicationOrderBundle, medicationOrder);
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertThat(drugOrder.getQuantity(), is(192.0));
        assertEquals(conceptService.getConcept(50), drugOrder.getQuantityUnits());
    }

    @Test
    public void shouldMapAdditionalInstructionsAndNotes() throws Exception {
        Order order = getOrder(medicationOrderBundle, medicationOrder);
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertTrue(StringUtils.isNotBlank(drugOrder.getDosingInstructions()));
        assertEquals("additional instructions notes", readFromJson(drugOrder.getDosingInstructions(), MRSProperties.BAHMNI_DRUG_ORDER_ADDITIONAL_INSTRCTIONS_KEY));
        String instructionsConceptName = readFromJson(drugOrder.getDosingInstructions(), MRSProperties.BAHMNI_DRUG_ORDER_INSTRCTIONS_KEY);
        assertEquals(conceptService.getConcept(1101).getName().getName(), instructionsConceptName);
    }

    private String readFromJson(String json, String key) throws IOException {
        Map map = new ObjectMapper().readValue(json, Map.class);
        return (String) map.get(key);
    }

    private Order getOrder(Bundle bundle, MedicationOrder resource) {
        Encounter mappedEncounter = new Encounter();
        Patient patient = new Patient();
        mapper.map(bundle, resource, patient, mappedEncounter);

        assertEquals(1, mappedEncounter.getOrders().size());
        return mappedEncounter.getOrders().iterator().next();
    }
}