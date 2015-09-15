package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.composite.TimingDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
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
        MedicationOrder prescription = (MedicationOrder) fhirResources.get(0).getResource();
        Date expectedDate = DateUtil.parseDate("2008-08-19 12:20:22");
        assertMedicationPrescription(prescription, expectedDate);
        assertEquals(fhirEncounter.getId().getValue(), prescription.getEncounter().getReference().getValue());
        assertEquals(1, prescription.getDosageInstruction().size());
        MedicationOrder.DosageInstruction dosageInstruction = prescription.getDosageInstruction().get(0);
        assertRoute(dosageInstruction);
        assertDoseQuantity(dosageInstruction);
        assertSchedule(dosageInstruction, 1, 1, UnitsOfTimeEnum.D,
                DateUtil.parseDate("2008-08-08"), DateUtil.parseDate("2008-08-13 23:59:59"));
    }

    @Test
    public void shouldCalculateSchedulesForTwiceAWeek() throws Exception {
        Order order = orderService.getOrder(17);
        Encounter fhirEncounter = getFhirEncounter();

        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder prescription = (MedicationOrder) fhirResources.get(0).getResource();
        assertSchedule(prescription.getDosageInstruction().get(0), 2, 1, UnitsOfTimeEnum.WK,
                DateUtil.parseDate("2008-08-08 00:00:00"), DateUtil.parseDate("2008-10-16 23:59:59"));
    }

    @Test
    public void shouldCalculateSchedulesForEveryThreeHoursFor10Weeks() throws Exception {
        Order order = orderService.getOrder(18);
        Encounter fhirEncounter = getFhirEncounter();

        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder prescription = (MedicationOrder) fhirResources.get(0).getResource();
        assertSchedule(prescription.getDosageInstruction().get(0), 1, 3, UnitsOfTimeEnum.H,
                DateUtil.parseDate("2008-08-08 00:00:00"), DateUtil.parseDate("2008-10-16 23:59:59"));
    }

    @Test
    public void shouldCalculateSchedulesForEveryTwoHoursFor48Hours() throws Exception {
        Order order = orderService.getOrder(19);
        Encounter fhirEncounter = getFhirEncounter();

        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder prescription = (MedicationOrder) fhirResources.get(0).getResource();
        assertSchedule(prescription.getDosageInstruction().get(0), 1, 2, UnitsOfTimeEnum.H,
                DateUtil.parseDate("2008-08-08 00:00:00"), DateUtil.parseDate("2008-10-09 23:59:59"));
    }

    @Test
    public void shouldCalculateBoundPeriodFromScheduledDate() throws Exception {
        Order order = orderService.getOrder(20);
        Encounter fhirEncounter = getFhirEncounter();

        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder prescription = (MedicationOrder) fhirResources.get(0).getResource();
        assertSchedule(prescription.getDosageInstruction().get(0), 2, 1, UnitsOfTimeEnum.WK,
                DateUtil.parseDate("2008-08-10 00:00:00"), DateUtil.parseDate("2008-10-18 23:59:59"));
    }

    @Test
    public void shouldSetAsNeeded() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();

        Order order = orderService.getOrder(20);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder prescription = (MedicationOrder) fhirResources.get(0).getResource();
        assertTrue(((BooleanDt) prescription.getDosageInstruction().get(0).getAsNeeded()).getValue());

        order = orderService.getOrder(19);
        fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        prescription = (MedicationOrder) fhirResources.get(0).getResource();
        assertFalse(((BooleanDt) prescription.getDosageInstruction().get(0).getAsNeeded()).getValue());
    }

    @Test
    public void shouldSetAdditionalInstructions() throws Exception {

    }

    private Encounter getFhirEncounter() {
        Encounter fhirEncounter = new Encounter();
        fhirEncounter.setId("shrEncId");
        fhirEncounter.setPatient(new ResourceReferenceDt().setReference("hid"));
        Encounter.Participant encounterParticipantComponent = fhirEncounter.addParticipant();
        encounterParticipantComponent.setIndividual(new ResourceReferenceDt().setReference("provider"));
        return fhirEncounter;
    }

    private void assertSchedule(MedicationOrder.DosageInstruction dosageInstruction, int expectedFrequency, int expectedPeriod, UnitsOfTimeEnum expectedPeriodUnits, Date expectedStartDate, Date expectedEndDate) throws ParseException {
        TimingDt timing = dosageInstruction.getTiming();
        assertNotNull(timing);
        TimingDt.Repeat repeat = timing.getRepeat();
        assertNotNull(repeat);
        PeriodDt bounds = (PeriodDt) repeat.getBounds();
        assertEquals(expectedStartDate, bounds.getStart());
        assertEquals(expectedEndDate, bounds.getEnd());
        assertNull(repeat.getDuration());
        assertTrue(expectedFrequency == repeat.getFrequency());
        assertEquals(new BigDecimal(expectedPeriod), repeat.getPeriod());
        assertEquals(expectedPeriodUnits, repeat.getPeriodUnitsElement().getValueAsEnum());
    }

    private void assertDoseQuantity(MedicationOrder.DosageInstruction dosageInstruction) {
        QuantityDt doseQuantity = (QuantityDt) dosageInstruction.getDose();
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

    private void assertMedicationPrescription(MedicationOrder prescription, Date expectedDate) {
        assertNotNull(prescription);
        assertEquals("hid", prescription.getPatient().getReference().getValue());
        assertNotNull(prescription.getIdentifier());

        assertEquals(expectedDate, prescription.getDateWritten());
        List<CodingDt> coding = ((CodeableConceptDt) prescription.getMedication()).getCoding();
        assertEquals(1, coding.size());
        assertEquals("drugs/104", coding.get(0).getSystem());
        assertEquals("Lactic Acid", coding.get(0).getDisplay());
        assertEquals("104", coding.get(0).getCode());
        assertTrue(prescription.getPrescriber().getReference().getValue().endsWith("321.json"));
    }
}