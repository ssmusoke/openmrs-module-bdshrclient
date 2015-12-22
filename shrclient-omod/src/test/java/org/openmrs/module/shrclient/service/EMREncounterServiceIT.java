package org.openmrs.module.shrclient.service;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import com.sun.syndication.feed.atom.Category;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.model.EncounterIdMapping;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.FhirBundleContextHolder;
import org.openmrs.module.shrclient.web.controller.dto.EncounterEvent;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.openmrs.module.shrclient.model.IdMappingType.ENCOUNTER;
import static org.openmrs.module.shrclient.web.controller.dto.EncounterEvent.ENCOUNTER_UPDATED_CATEGORY_TAG;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EMREncounterServiceIT extends BaseModuleWebContextSensitiveTest {
    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private PatientService patientService;

    @Autowired
    EMREncounterService emrEncounterService;

    @Autowired
    EncounterService encounterService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private IdMappingRepository idMappingRepository;


    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/omrsGlobalPropertyTestDS.xml");
    }

    @Test
    public void shouldSaveEncounter() throws Exception {
        executeDataSet("testDataSets/shrClientEncounterReverseSyncTestDS.xml");
        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        String shrEncounterId = "shr-enc-id";
        List<EncounterEvent> bundles = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/testFHIREncounter.xml");
        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles);

        EncounterIdMapping idMapping = (EncounterIdMapping) idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        assertEquals(1, encounter.getEncounterProviders().size());
        assertEquals(providerService.getProvider(22), encounter.getEncounterProviders().iterator().next().getProvider());
    }

    @Test
    public void shouldProcessDeathInfoOfPatientAfterEncounterSave() throws Exception {
        executeDataSet("testDataSets/patientDeathNoteDS.xml");

        Patient patient = patientService.getPatient(1);
        List<EncounterEvent> bundles = getEncounterEvents("shrEncounterId", "classpath:encounterBundles/dstu2/encounterWithDiagnosticOrder.xml");

        assertEquals(true, patient.isDead());
        assertEquals("Unspecified Cause Of Death", patient.getCauseOfDeath().getName().getName());

        emrEncounterService.createOrUpdateEncounter(patient, bundles.get(0));

        assertEquals(true, patient.isDead());
        assertEquals("HIV", patient.getCauseOfDeath().getName().getName());
    }

    @Test
    public void shouldSaveTestOrders() throws Exception {
        executeDataSet("testDataSets/shrDiagnosticOrderSyncTestDS.xml");
        String shrEncounterId = "shr-enc-id";

        List<EncounterEvent> bundles = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/encounterWithDiagnosticOrder.xml");
        Patient emrPatient = patientService.getPatient(1);
        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles);

        EncounterIdMapping encounterIdMapping = (EncounterIdMapping) idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        assertNotNull(encounterIdMapping);
        Encounter encounter = encounterService.getEncounterByUuid(encounterIdMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
    }

    @Test
    @Ignore("Ignored because of a bug on OpenMRS which doesn't let you save a already expired order")
    public void shouldDiscontinueATestOrderIfUpdated() throws Exception {
        executeDataSet("testDataSets/shrDiagnosticOrderSyncTestDS.xml");
        String healthId = "HIDA764177";
        String shrEncounterId = "shr-enc-id";

        List<EncounterEvent> bundleWithNewTestOrder = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/encounterWithDiagnosticOrder.xml");
        Patient emrPatient = patientService.getPatient(1);
        emrEncounterService.createOrUpdateEncounters(emrPatient, bundleWithNewTestOrder);

        IdMapping idMapping = idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        Order firstOrder = orders.iterator().next();
        assertNull(firstOrder.getDateStopped());
        assertEquals(Order.Action.NEW, firstOrder.getAction());
        List<EncounterEvent> bundleWithCancelledTestOrder = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/encounterWithCancelledDiagnosticOrder.xml");

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundleWithCancelledTestOrder);

        IdMapping updatedIdMapping = idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        assertNotNull(updatedIdMapping);
        assertTrue(updatedIdMapping.getLastSyncDateTime().after(idMapping.getLastSyncDateTime()));
        Encounter updatedEncounter = encounterService.getEncounterByUuid(updatedIdMapping.getInternalId());
        Set<Order> updatedEncounterOrders = updatedEncounter.getOrders();
        assertFalse(updatedEncounterOrders.isEmpty());
        assertEquals(2, updatedEncounterOrders.size());
        Order discontinuedOrder = getDiscontinuedOrder(updatedEncounterOrders);
        assertNotNull(discontinuedOrder);
        assertEquals(firstOrder, discontinuedOrder.getPreviousOrder());
        assertNotNull(firstOrder.getDateStopped());
    }

    private Order getDiscontinuedOrder(Set<Order> updatedEncounterOrders) {
        for (Order order : updatedEncounterOrders) {
            if (Order.Action.DISCONTINUE.equals(order.getAction())) return order;
        }
        return null;
    }

    @Test
    public void shouldSaveDrugOrders() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String shrEncounterId = "shr-enc-id";

        List<EncounterEvent> bundles = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/encounterWithMedicationOrder.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles);

        IdMapping idMapping = idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    public void shouldSaveDrugOrderWithCustomDosageAndStoppedOrder() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String shrEncounterId = "shr-enc-id";

        List<EncounterEvent> bundles = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/encounterWithStoppedMedicationOrderAndCustomDosage.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles);

        IdMapping idMapping = idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    public void shouldSaveDrugOrderWithScheduledDate() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String shrEncounterId = "shr-enc-id";

        List<EncounterEvent> bundles = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/encounterWithMedicationOrderWithScheduledDate.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles);

        IdMapping idMapping = idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    @Ignore("Ignored because of a bug on OpenMRS which doesn't let you revise a retrospective drug order edit")
    public void shouldSaveDrugOrderEditedInDifferentEncounter() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String shrEncounterId = "shr-enc-id";

        List<EncounterEvent> bundles = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/encounterWithMedicationOrderEditedInDifferentEncounter.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles);

        IdMapping idMapping = idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    @Ignore("Ignored because of a bug on OpenMRS which doesn't let you revise a retrospective drug order edit")
    public void shouldSaveDrugOrderEditedInSameEncounter() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String shrEncounterId = "shr-enc-id";

        List<EncounterEvent> bundles = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/encounterWithMedicationOrderEditedInSameEncounter.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles);

        IdMapping idMapping = idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(2, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    public void shouldSaveAMedicationOrderWithoutDoseRoutes() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String shrEncounterId = "shr-enc-id";

        List<EncounterEvent> bundles = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/medicationOrderWithoutDoseRouteAndAdditionalInstructions.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles);

        IdMapping idMapping = idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    public void shouldUpdateTheSameEncounter() throws Exception {
        executeDataSet("testDataSets/shrClientEncounterReverseSyncTestDS.xml");
        Patient patient = patientService.getPatient(1);
        String shrEncounterId = "shr-enc-id";
        List<EncounterEvent> events = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/testFHIREncounter.xml");

        Date currentTime = new Date();
        Date tenMinutesAfter = getDateTimeAfterNMinutes(currentTime, 10);

        Category category = new Category();
        category.setTerm(ENCOUNTER_UPDATED_CATEGORY_TAG + ":" + DateUtil.toISOString(currentTime));
        events.get(0).setCategories(asList(category));

        emrEncounterService.createOrUpdateEncounters(patient, events);
        EncounterIdMapping mapping = (EncounterIdMapping) idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        String encounterUUID = mapping.getInternalId();
        Date firstServerUpdateDateTime = mapping.getServerUpdateDateTime();

        Category newCategory = new Category();
        newCategory.setTerm(ENCOUNTER_UPDATED_CATEGORY_TAG + ":" + DateUtil.toISOString(tenMinutesAfter));
        events.get(0).setCategories(asList(newCategory));

        emrEncounterService.createOrUpdateEncounters(patient, events);
        mapping = (EncounterIdMapping) idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        Encounter encounter2 = encounterService.getEncounterByUuid(mapping.getInternalId());

        assertEquals(encounterUUID, encounter2.getUuid());
        assertTrue(firstServerUpdateDateTime.before(mapping.getServerUpdateDateTime()));
    }

    @Test
    public void shouldVoidOlderObservationsAndRecreateWithNewValues() throws Exception {
        executeDataSet("testDataSets/shrClientEncounterWithObservationTestDs.xml");
        Patient patient = patientService.getPatient(1);
        String shrEncounterId = "shr-enc-id";

        List<EncounterEvent> bundles1 = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/encounterWithObservations.xml");
        emrEncounterService.createOrUpdateEncounters(patient, bundles1);
        IdMapping mapping = idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        Encounter encounter = encounterService.getEncounterByUuid(mapping.getInternalId());

        Set<Obs> topLevelObs = encounter.getObsAtTopLevel(true);
        Set<Obs> allObs = encounter.getAllObs(true);
        assertEquals(1, topLevelObs.size());
        assertEquals(3, allObs.size());
        Obs diastolicBp = topLevelObs.iterator().next().getGroupMembers().iterator().next().getGroupMembers().iterator().next();
        assertEquals(new Double(70.0), diastolicBp.getValueNumeric());

        List<EncounterEvent> bundles2 = getEncounterEvents(shrEncounterId, "encounterBundles/dstu2/encounterWithUpdatedObservations.xml");
        emrEncounterService.createOrUpdateEncounters(patient, bundles2);
        mapping = idMappingRepository.findByExternalId(shrEncounterId, ENCOUNTER);
        encounter = encounterService.getEncounterByUuid(mapping.getInternalId());

        assertEquals(2, encounter.getObsAtTopLevel(true).size());
        assertEquals(6, encounter.getAllObs(true).size());
        topLevelObs = encounter.getObsAtTopLevel(false);
        assertEquals(1, topLevelObs.size());
        assertEquals(3, encounter.getAllObs(false).size());
        diastolicBp = topLevelObs.iterator().next().getGroupMembers().iterator().next().getGroupMembers().iterator().next();
        assertEquals(new Double(120.0), diastolicBp.getValueNumeric());
    }

    @Test
    public void shouldRetryIfFailsToDownloadAnEncounter() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        Patient patient = patientService.getPatient(110);
        String shrEncounterId1 = "shr-enc-id1";
        String shrEncounterId2 = "shr-enc-id2";

        EncounterEvent bundle1 = getEncounterEvents(shrEncounterId1, "encounterBundles/dstu2/medicationOrderWithPriorPrescription.xml").get(0);
        EncounterEvent bundle2 = getEncounterEvents(shrEncounterId2, "encounterBundles/dstu2/encounterWithMedicationOrder.xml").get(0);
        List<EncounterEvent> encounterEvents = asList(bundle1, bundle2);
        emrEncounterService.createOrUpdateEncounters(patient, encounterEvents);

        IdMapping mapping1 = idMappingRepository.findByExternalId(shrEncounterId1, ENCOUNTER);
        IdMapping mapping2 = idMappingRepository.findByExternalId(shrEncounterId2, ENCOUNTER);

        assertTrue(mapping1.getLastSyncDateTime().after(mapping2.getLastSyncDateTime()));
    }

    public Date getDateTimeAfterNMinutes(Date currentTime, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentTime);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    private List<EncounterEvent> getEncounterEvents(String shrEncounterId, String encounterBundleFilePath) throws Exception {
        List<EncounterEvent> events = new ArrayList<>();
        EncounterEvent encounterEvent = new EncounterEvent();
        String publishedDate = DateUtil.toISOString(DateTime.now().toDate());
        Category category = new Category();
        category.setTerm(ENCOUNTER_UPDATED_CATEGORY_TAG + ":" + publishedDate);
        encounterEvent.setCategories(asList(category));
        encounterEvent.addContent((Bundle) loadSampleFHIREncounter(encounterBundleFilePath, springContext));
        encounterEvent.setTitle("Encounter:" + shrEncounterId);
        encounterEvent.setLink("http://shr.com/patients/" + encounterEvent.getHealthId() + "/encounters/" + shrEncounterId);
        events.add(encounterEvent);
        return events;
    }

    public IResource loadSampleFHIREncounter(String filePath, ApplicationContext springContext) throws Exception {
        org.springframework.core.io.Resource resource = springContext.getResource(filePath);
        String bundleXML = org.apache.commons.io.IOUtils.toString(resource.getInputStream());
        return (IResource) FhirBundleContextHolder.getFhirContext().newXmlParser().parseResource(bundleXML);
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

}