package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.DurationDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.composite.SimpleQuantityDt;
import ca.uhn.fhir.model.dstu2.composite.TimingDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
<<<<<<< HEAD
import org.junit.After;
=======
import ca.uhn.fhir.model.primitive.DateTimeDt;
>>>>>>> Neha | bdshr-772 | using duration for MedicationOrder.bounds, added extention for scheduled date
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DrugOrderMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private DrugOrderMapper orderMapper;

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }
    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
    }

    @Test
    public void shouldMapDrugOrders() throws Exception {
        Order order = orderService.getOrder(16);
        Encounter fhirEncounter = getFhirEncounter();

        assertTrue(orderMapper.canHandle(order));
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));

        assertEquals(1, fhirResources.size());
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        Date expectedDate = DateUtil.parseDate("2008-08-19 12:20:22");
        assertMedicationOrder(medicationOrder, expectedDate);
        assertEquals(fhirEncounter.getId().getValue(), medicationOrder.getEncounter().getReference().getValue());
        assertEquals(1, medicationOrder.getDosageInstruction().size());
        MedicationOrder.DosageInstruction dosageInstruction = medicationOrder.getDosageInstruction().get(0);
        assertRoute(dosageInstruction);
        assertDoseQuantity(dosageInstruction);
        assertSchedule(dosageInstruction, 1, 1, UnitsOfTimeEnum.D,
                6, UnitsOfTimeEnum.D);
    }

    @Test
    public void shouldCalculateSchedulesForTwiceAWeek() throws Exception {
        Order order = orderService.getOrder(17);
        Encounter fhirEncounter = getFhirEncounter();

        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        assertSchedule(medicationOrder.getDosageInstruction().get(0), 2, 1, UnitsOfTimeEnum.WK,
                10, UnitsOfTimeEnum.WK);
    }

    @Test
    public void shouldCalculateSchedulesForEveryThreeHoursFor10Weeks() throws Exception {
        Order order = orderService.getOrder(18);
        Encounter fhirEncounter = getFhirEncounter();

        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        assertSchedule(medicationOrder.getDosageInstruction().get(0), 1, 3, UnitsOfTimeEnum.H,
                10, UnitsOfTimeEnum.WK);
    }

    @Test
    public void shouldCalculateSchedulesForEveryTwoHoursFor48Hours() throws Exception {
        Order order = orderService.getOrder(19);
        Encounter fhirEncounter = getFhirEncounter();

        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        assertSchedule(medicationOrder.getDosageInstruction().get(0), 1, 2, UnitsOfTimeEnum.H,
                2, UnitsOfTimeEnum.D);
    }

    @Test
    public void shouldSetScheduledDate() throws Exception {
        Order order = orderService.getOrder(20);
        Encounter fhirEncounter = getFhirEncounter();

        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        MedicationOrder.DosageInstruction dosageInstruction = medicationOrder.getDosageInstruction().get(0);
        ExtensionDt scheduledDateExtension = dosageInstruction.getTiming().getUndeclaredExtensions().get(0);
        assertEquals(order.getScheduledDate(), ((DateTimeDt) scheduledDateExtension.getValue()).getValue());
        assertEquals(FHIRProperties.SCHEDULED_DATE_EXTENSION_URL, scheduledDateExtension.getUrl());
    }

    @Test
    public void shouldSetAsNeeded() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();

        Order order = orderService.getOrder(20);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        assertTrue(((BooleanDt) medicationOrder.getDosageInstruction().get(0).getAsNeeded()).getValue());

        order = orderService.getOrder(19);
        fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        assertFalse(((BooleanDt) medicationOrder.getDosageInstruction().get(0).getAsNeeded()).getValue());
    }

    private Encounter getFhirEncounter() {
        Encounter fhirEncounter = new Encounter();
        fhirEncounter.setId("shrEncId");
        fhirEncounter.setPatient(new ResourceReferenceDt().setReference("hid"));
        Encounter.Participant encounterParticipantComponent = fhirEncounter.addParticipant();
        encounterParticipantComponent.setIndividual(new ResourceReferenceDt().setReference("provider"));
        return fhirEncounter;
    }

    private void assertSchedule(MedicationOrder.DosageInstruction dosageInstruction, int expectedFrequency, int expectedPeriod, UnitsOfTimeEnum expectedPeriodUnits, int expectedDuration, UnitsOfTimeEnum expectedDurationUnits) throws ParseException {
        TimingDt timing = dosageInstruction.getTiming();
        assertNotNull(timing);
        TimingDt.Repeat repeat = timing.getRepeat();
        assertNotNull(repeat);
        DurationDt bounds = (DurationDt) repeat.getBounds();
        assertEquals(expectedDuration, bounds.getValue().intValue());
        assertEquals(expectedDurationUnits.getCode(), bounds.getCode());
        assertNull(repeat.getDuration());
        assertTrue(expectedFrequency == repeat.getFrequency());
        assertEquals(new BigDecimal(expectedPeriod), repeat.getPeriod());
        assertEquals(expectedPeriodUnits, repeat.getPeriodUnitsElement().getValueAsEnum());
    }

    private void assertDoseQuantity(MedicationOrder.DosageInstruction dosageInstruction) {
        assertTrue(dosageInstruction.getDose() instanceof SimpleQuantityDt);
        SimpleQuantityDt doseQuantity = (SimpleQuantityDt) dosageInstruction.getDose();
        assertNotNull(doseQuantity);
        assertEquals("http://localhost:9080/openmrs/ws/rest/v1/tr/vs/Quantity-Units", doseQuantity.getSystem());
        assertEquals("mg", doseQuantity.getCode());
        assertTrue(4 == doseQuantity.getValue().doubleValue());
    }

    private void assertRoute(MedicationOrder.DosageInstruction dosageInstruction) {
        CodingDt routeCode = dosageInstruction.getRoute().getCoding().get(0);
        assertEquals("Oral", routeCode.getDisplay());
        assertEquals("Oral", routeCode.getCode());
        assertEquals("http://localhost:9080/openmrs/ws/rest/v1/tr/vs/Route-of-Administration", routeCode.getSystem());
    }

    private void assertMedicationOrder(MedicationOrder medicationOrder, Date expectedDate) {
        assertNotNull(medicationOrder);
        assertEquals("hid", medicationOrder.getPatient().getReference().getValue());
        assertNotNull(medicationOrder.getIdentifier());

        assertEquals(expectedDate, medicationOrder.getDateWritten());
        List<CodingDt> coding = ((CodeableConceptDt) medicationOrder.getMedication()).getCoding();
        assertEquals(1, coding.size());
        assertEquals("drugs/104", coding.get(0).getSystem());
        assertEquals("Lactic Acid", coding.get(0).getDisplay());
        assertEquals("104", coding.get(0).getCode());
        assertTrue(medicationOrder.getPrescriber().getReference().getValue().endsWith("321.json"));
    }
}