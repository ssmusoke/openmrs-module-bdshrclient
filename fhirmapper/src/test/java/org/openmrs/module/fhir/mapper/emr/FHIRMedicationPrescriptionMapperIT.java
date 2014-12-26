package org.openmrs.module.fhir.mapper.emr;

import ch.lambdaj.Lambda;
import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.TestHelper;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static java.lang.Math.abs;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class FHIRMedicationPrescriptionMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private FHIRMedicationPrescriptionMapper mapper;

    @Autowired
    private ConceptService conceptService;

    private Resource resource;
    private AtomFeed feed;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");

        ParserBase.ResourceOrFeed resourceOrFeed = new TestHelper().loadSampleFHIREncounter("encounterBundles/encounterWithMedicationPrescription.xml", springContext);
        feed = resourceOrFeed.getFeed();
        resource = FHIRFeedHelper.identifyResource(feed.getEntryList(), ResourceType.MedicationPrescription);
    }

    @Test
    public void shouldHandleResourceOfTypeMedicationPrescription() throws Exception {
        assertTrue(mapper.canHandle(resource));
    }

    @Test
    public void shouldMapMedicationToDrug(){
        Encounter mappedEncounter = new Encounter();
        mapper.map(feed, resource, new Patient(), mappedEncounter,new HashMap<String, List<String>>());

        assertEquals(1, mappedEncounter.getOrders().size());
        Order order = mappedEncounter.getOrders().iterator().next();
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertEquals(conceptService.getDrug(301), drugOrder.getDrug());
        assertEquals(conceptService.getConcept(301), drugOrder.getConcept());
    }

    @Test
    public void shouldMapPatientToDrug(){
        Encounter mappedEncounter = new Encounter();
        Patient patient = new Patient();
        mapper.map(feed, resource, patient, mappedEncounter,new HashMap<String, List<String>>());

        assertEquals(1, mappedEncounter.getOrders().size());
        Order order = mappedEncounter.getOrders().iterator().next();
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertEquals(patient, drugOrder.getPatient());
    }

    @Test
    public void shouldMapFrequencyAndDurationToDrug(){
        Encounter mappedEncounter = new Encounter();
        Patient patient = new Patient();
        mapper.map(feed, resource, patient, mappedEncounter,new HashMap<String, List<String>>());

        assertEquals(1, mappedEncounter.getOrders().size());
        Order order = mappedEncounter.getOrders().iterator().next();
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertEquals(2, drugOrder.getDuration().intValue());
        assertEquals(conceptService.getConcept(803), drugOrder.getDurationUnits());

        assertEquals(conceptService.getConcept(902), drugOrder.getFrequency().getConcept());
    }

    @Test
    public void shouldMapDrugDosageAndRoutes(){
        Encounter mappedEncounter = new Encounter();
        mapper.map(feed, resource, new Patient(), mappedEncounter,new HashMap<String, List<String>>());

        assertEquals(1, mappedEncounter.getOrders().size());
        Order order = mappedEncounter.getOrders().iterator().next();
        assertTrue(order instanceof DrugOrder);
        DrugOrder drugOrder = (DrugOrder) order;

        assertEquals(new Double(3), drugOrder.getDose());
        assertEquals(conceptService.getConcept(806), drugOrder.getDoseUnits());
        assertEquals(conceptService.getConcept(701), drugOrder.getRoute());
    }
    public class TestClass123 {
        private double a;

        public TestClass123(double a) {
            this.a = a;
        }

        public double getA() {
            return a;
        }
    }
    @Test
    public void blah(){
        List<TestClass123> numbers = Arrays.asList(new TestClass123(3.0), new TestClass123(4.0), new TestClass123(1.0), new TestClass123(2.9));
        System.out.println(Lambda.select(extract(numbers, on(TestClass123.class).getA()), is(closeTo(1.1, 0.5))));
//        System.out.println(getClosestValue(numbers));
    }

    private boolean getClosestValue(List<TestClass123> numbers) {
        List<TestClass123> sort = Lambda.sort(numbers, on(TestClass123.class).getA());
        double t = 5;
        double l = sort.get(0).getA() - t;

        TestClass123 testClass = null;
        for (TestClass123 testClass123 : sort) {
            double a = testClass123.getA() - t;
            if(abs(a) <= abs(l)) {
                testClass = testClass123;
                l = a;
            }
        }
        System.out.println(testClass.getA());
        return true;
    }
}