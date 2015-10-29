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
import ca.uhn.fhir.model.dstu2.valueset.MedicationOrderStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.TimingAbbreviationEnum;
import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.FHIRProperties;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.containsCoding;
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
    public void shouldMapMedicationOrderDateAndRoute() throws Exception {
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
        assertTrue(containsCoding(dosageInstruction.getRoute().getCoding(),
                "Oral", "http://localhost:9080/openmrs/ws/rest/v1/tr/vs/Route-of-Administration", "Oral"));
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
        assertTrue(scheduledDateExtension.getUrl().endsWith(FHIRProperties.SCHEDULED_DATE_EXTENSION_NAME));
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

    @Test
    public void shouldMapDoseFromMedicationFormsValueset() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();
        Order order = orderService.getOrder(19);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));

        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        assertDoseQuantity(medicationOrder.getDosageInstruction().get(0), "http://localhost:9080/openmrs/ws/rest/v1/tr/vs/Medication-Forms", "Pill", "Pill");
    }

    @Test
    public void shouldMapDoseFromMedicationPackageFormsFormsValueset() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();
        Order order = orderService.getOrder(18);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));

        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        assertDoseQuantity(medicationOrder.getDosageInstruction().get(0), "http://localhost:9080/openmrs/ws/rest/v1/tr/vs/Medication-Package-Forms", "Puffs", "Puffs");
    }

    @Test
    public void shouldNotSetSystemAndCodeIfDoseFromQuantityUnits() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();
        Order order = orderService.getOrder(17);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));

        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        assertDoseQuantity(medicationOrder.getDosageInstruction().get(0), null, null, "mg");
    }

    @Test
    public void shouldSetDispenseRequestForLocalQuantity() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();

        Order order = orderService.getOrder(20);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        SimpleQuantityDt quantity = medicationOrder.getDispenseRequest().getQuantity();
        assertThat(quantity.getValue().doubleValue(), is(190.0));
        assertEquals("mg", quantity.getUnit());
    }

    @Test
    public void shouldMapAdditionalInstructionsAndNotes() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();

        Order order = orderService.getOrder(21);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        assertTrue(containsCoding(medicationOrder.getDosageInstruction().get(0).getAdditionalInstructions().getCoding(),
                "1101", "/concepts/1101", "As directed"));

        assertEquals("additional instructions notes", medicationOrder.getNote());
    }

    @Test
    public void shouldMapMorningAfternoonAndNightDose() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();

        Order order = orderService.getOrder(22);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();

        assertEquals(1, medicationOrder.getDosageInstruction().size());
        MedicationOrder.DosageInstruction dosageInstruction = medicationOrder.getDosageInstructionFirstRep();
        List<ExtensionDt> extensions = dosageInstruction.getUndeclaredExtensionsByUrl(FHIRProperties.getFhirExtensionUrl(FHIRProperties.DOSAGEINSTRUCTION_CUSTOM_DOSAGE_EXTENSION_NAME));
        assertEquals(1, extensions.size());
        String value = ((StringDt) extensions.get(0).getValue()).getValue();
        Map map = new ObjectMapper().readValue(value, Map.class);
        assertEquals(3, map.size());
        assertEquals(1, map.get(FHIRProperties.FHIR_DRUG_ORDER_MORNING_DOSE_KEY));
        assertEquals(2, map.get(FHIRProperties.FHIR_DRUG_ORDER_AFTERNOON_DOSE_KEY));
        assertEquals(3, map.get(FHIRProperties.FHIR_DRUG_ORDER_EVENING_DOSE_KEY));
        assertEquals(TimingAbbreviationEnum.TID.getCode(), dosageInstruction.getTiming().getCode().getCodingFirstRep().getCode());
    }

    @Test
    public void shouldMapMorningAfternoonDoseOnly() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();

        Order order = orderService.getOrder(23);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();

        assertEquals(1, medicationOrder.getDosageInstruction().size());
        MedicationOrder.DosageInstruction dosageInstruction = medicationOrder.getDosageInstructionFirstRep();
        List<ExtensionDt> extensions = dosageInstruction.getUndeclaredExtensionsByUrl(FHIRProperties.getFhirExtensionUrl(FHIRProperties.DOSAGEINSTRUCTION_CUSTOM_DOSAGE_EXTENSION_NAME));
        assertEquals(1, extensions.size());
        String value = ((StringDt) extensions.get(0).getValue()).getValue();
        Map map = new ObjectMapper().readValue(value, Map.class);
        assertEquals(2, map.size());
        assertEquals(11, map.get(FHIRProperties.FHIR_DRUG_ORDER_MORNING_DOSE_KEY));
        assertEquals(12, map.get(FHIRProperties.FHIR_DRUG_ORDER_AFTERNOON_DOSE_KEY));
        assertNull(map.get(FHIRProperties.FHIR_DRUG_ORDER_EVENING_DOSE_KEY));
        assertEquals(TimingAbbreviationEnum.BID.getCode(), dosageInstruction.getTiming().getCode().getCodingFirstRep().getCode());
    }

    @Test
    public void shouldMapEveningDoseOnly() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();

        Order order = orderService.getOrder(24);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();

        assertEquals(1, medicationOrder.getDosageInstruction().size());
        MedicationOrder.DosageInstruction dosageInstruction = medicationOrder.getDosageInstructionFirstRep();
        List<ExtensionDt> extensions = dosageInstruction.getUndeclaredExtensionsByUrl(FHIRProperties.getFhirExtensionUrl(FHIRProperties.DOSAGEINSTRUCTION_CUSTOM_DOSAGE_EXTENSION_NAME));
        assertEquals(1, extensions.size());
        String value = ((StringDt) extensions.get(0).getValue()).getValue();
        Map map = new ObjectMapper().readValue(value, Map.class);
        assertEquals(1, map.size());
        assertNull(map.get(FHIRProperties.FHIR_DRUG_ORDER_MORNING_DOSE_KEY));
        assertNull(map.get(FHIRProperties.FHIR_DRUG_ORDER_AFTERNOON_DOSE_KEY));
        assertEquals(30, map.get(FHIRProperties.FHIR_DRUG_ORDER_EVENING_DOSE_KEY));
        assertEquals(TimingAbbreviationEnum.QD.getCode(), dosageInstruction.getTiming().getCode().getCodingFirstRep().getCode());
    }

    @Test
    public void shouldSetStatusAndDateEndedForStoppedDrugOrders() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();

        Order order = orderService.getOrder(25);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();

        assertEquals(MedicationOrderStatusEnum.STOPPED.getCode(), medicationOrder.getStatus());
        assertEquals(DateUtil.parseDate("2008-10-09 13:59:59"), medicationOrder.getDateEnded());
    }

    @Test
    public void shouldSetPreviousOrderReferenceForEditedDrugOrders() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();

        Order order = orderService.getOrder(26);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();

        assertEquals(MedicationOrderStatusEnum.ACTIVE.getCode(), medicationOrder.getStatus());
        assertEquals("urn:uuid:amkbja86-awaa-g1f3-9qw0-ccc2c6c63ab0", medicationOrder.getPriorPrescription().getReference().getValue());
    }

    @Test
    public void shouldSetPreviousOrderEncounterUrlForEditedDrugOrdersInDifferentEncounters() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();

        Order order = orderService.getOrder(27);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();

        assertEquals(MedicationOrderStatusEnum.ACTIVE.getCode(), medicationOrder.getStatus());
        assertEquals("encounters/shr_enc_id_1#MedicationOrder/amkbja86-awaa-g1f3-9qw0-ccc2c6c63ab0", medicationOrder.getPriorPrescription().getReference().getValue());
    }

    @Test
    public void shouldSetOrderActionExtension() throws Exception {
        Encounter fhirEncounter = getFhirEncounter();

        Order order = orderService.getOrder(24);
        List<FHIRResource> fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        MedicationOrder medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        ExtensionDt orderActionExtension = medicationOrder.getUndeclaredExtensionsByUrl(FHIRProperties.getFhirExtensionUrl(FHIRProperties.MEDICATIONORDER_ACTION_EXTENSION_NAME)).get(0);
        assertEquals(((StringDt)orderActionExtension.getValue()).getValue(), Order.Action.NEW.name());

        order = orderService.getOrder(25);
        fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        orderActionExtension = medicationOrder.getUndeclaredExtensionsByUrl(FHIRProperties.getFhirExtensionUrl(FHIRProperties.MEDICATIONORDER_ACTION_EXTENSION_NAME)).get(0);
        assertEquals(((StringDt)orderActionExtension.getValue()).getValue(), Order.Action.DISCONTINUE.name());

        order = orderService.getOrder(26);
        fhirResources = orderMapper.map(order, fhirEncounter, new Bundle(), getSystemProperties("1"));
        medicationOrder = (MedicationOrder) fhirResources.get(0).getResource();
        orderActionExtension = medicationOrder.getUndeclaredExtensionsByUrl(FHIRProperties.getFhirExtensionUrl(FHIRProperties.MEDICATIONORDER_ACTION_EXTENSION_NAME)).get(0);
        assertEquals(((StringDt)orderActionExtension.getValue()).getValue(), Order.Action.REVISE.name());
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

    private void assertDoseQuantity(MedicationOrder.DosageInstruction dosageInstruction, String valueSetUrl, String code, String displayUnit) {
        assertTrue(dosageInstruction.getDose() instanceof SimpleQuantityDt);
        SimpleQuantityDt doseQuantity = (SimpleQuantityDt) dosageInstruction.getDose();
        assertNotNull(doseQuantity);
        assertEquals(valueSetUrl, doseQuantity.getSystem());
        assertEquals(code, doseQuantity.getCode());
        assertEquals(displayUnit, doseQuantity.getUnit());
        assertTrue(4 == doseQuantity.getValue().doubleValue());
    }

    private void assertMedicationOrder(MedicationOrder medicationOrder, Date expectedDate) {
        assertNotNull(medicationOrder);
        assertEquals("hid", medicationOrder.getPatient().getReference().getValue());
        assertNotNull(medicationOrder.getIdentifier());

        assertEquals(expectedDate, medicationOrder.getDateWritten());
        List<CodingDt> coding = ((CodeableConceptDt) medicationOrder.getMedication()).getCoding();
        assertEquals(1, coding.size());
        assertTrue(containsCoding(coding, "104", "drugs/104", "Lactic Acid"));

        assertTrue(medicationOrder.getPrescriber().getReference().getValue().endsWith("321.json"));
    }
}