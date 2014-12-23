package org.openmrs.module.fhir.mapper.emr;

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

import java.util.HashMap;
import java.util.List;

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
        executeDataSet("drugOrderDS.xml");

        ParserBase.ResourceOrFeed resourceOrFeed = new TestHelper().loadSampleFHIREncounter("encounterBundles/encounterWithMedicationPrescription.xml", springContext);
        feed = resourceOrFeed.getFeed();
        resource = FHIRFeedHelper.identifyResource(feed.getEntryList(), ResourceType.MedicationPrescription);
    }

    @Test
    public void shouldHandleResourceOfTypeMedicationPrescription() throws Exception {
        assertTrue(mapper.canHandle(resource));
    }

    @Test
    public void shouldMapMedicationPrescriptionResourceToDrugOrders(){
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
}