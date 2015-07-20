package org.openmrs.module.shrclient.service.impl;

import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.formats.XmlParser;
import org.hl7.fhir.instance.model.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.mapper.MRSProperties.GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH;
import static org.openmrs.module.fhir.mapper.MRSProperties.GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class MciPatientServiceImplIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private PatientService patientService;

    @Autowired
    MciPatientService mciPatientService;

    @Autowired
    EncounterService encounterService;

    @Autowired
    ConceptService conceptService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/omrsGlobalPropertyTestDS.xml");
    }

    @Test
    public void shouldSaveEncounter() throws Exception {
        executeDataSet("testDataSets/shrClientEncounterReverseSyncTestDS.xml");
        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        String healthId = "HIDA764177";
        String shrEncounterId = "shr-enc-id";
        Map<String, Concept> conceptCache = omrsConceptLookup.getConceptsConfiguredViaGlobalProperties(Arrays.asList(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH));

        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "classpath:encounterBundles/testFHIREncounter.xml");
        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId, conceptCache);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        assertEquals(1, encounter.getEncounterProviders().size());
        assertEquals(providerService.getProvider(22), encounter.getEncounterProviders().iterator().next().getProvider());
    }

    @Test
    public void shouldProcessDeathInfoOfPatientAfterEncounterSave() throws Exception {
        executeDataSet("testDataSets/patientDeathNoteDS.xml");
        Map<String, Concept> conceptCache = omrsConceptLookup.getConceptsConfiguredViaGlobalProperties(Arrays.asList(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH));

        Patient patient = patientService.getPatient(1);
        List<EncounterBundle> bundles = getEncounterBundles("healthId", "shrEncounterId", "classpath:encounterBundles/encounterWithDiagnosticOrder.xml");

        assertEquals(true, patient.isDead());
        assertEquals("Unspecified Cause Of Death", patient.getCauseOfDeath().getName().getName());

        mciPatientService.createOrUpdateEncounter(patient, bundles.get(0), "healthId", conceptCache);

        assertEquals(true, patient.isDead());
        assertEquals("HIV", patient.getCauseOfDeath().getName().getName());
    }

    @Test
    public void shouldSaveTestOrders() throws Exception {
        executeDataSet("testDataSets/shrDiagnosticOrderSyncTestDS.xml");
        String healthId = "5915668841731457025";
        String shrEncounterId = "shr-enc-id";
        Map<String, Concept> conceptCache = omrsConceptLookup.getConceptsConfiguredViaGlobalProperties(Arrays.asList(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH));

        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "classpath:encounterBundles/encounterWithDiagnosticOrder.xml");
        Patient emrPatient = patientService.getPatient(1);
        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId, conceptCache);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof TestOrder);
    }

    @Test
    public void shouldSaveDrugOrders() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String healthId = "5947482439084408833";
        String shrEncounterId = "shr-enc-id";
        Map<String, Concept> conceptCache = omrsConceptLookup.getConceptsConfiguredViaGlobalProperties(Arrays.asList(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH));

        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/encounterWithMedicationPrescription.xml");
        Patient emrPatient = patientService.getPatient(110);
        assertEquals(0, encounterService.getEncountersByPatient(emrPatient).size());

        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId, conceptCache);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    public void shouldGetCauseOfDeathOfPatientIfAnyObservationCapturedCauseOfDeath() throws Exception {
        executeDataSet("testDataSets/patientDeathNoteDS.xml");
        Map<String, Concept> conceptCache = omrsConceptLookup.getConceptsConfiguredViaGlobalProperties(Arrays.asList(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH));
        Patient patient = patientService.getPatient(1);

        Concept actualCauseOfDeath = mciPatientService.getCauseOfDeath(patient, conceptCache);

        assertEquals("HIV", actualCauseOfDeath.getName().getName());

    }

    @Test
    public void shouldReturnUnspecifiedCauseOfDeathIfThereIsNoObservationCapturedAndPatientIsToBeMarkedDead() throws Exception {
        executeDataSet("testDataSets/patientDeathNoteDS.xml");
        Map<String, Concept> conceptCache = omrsConceptLookup.getConceptsConfiguredViaGlobalProperties(Arrays.asList(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH));
        Patient patient = patientService.getPatient(4);

        patient.setDead(true);
        Concept actualCauseOfDeath = mciPatientService.getCauseOfDeath(patient, conceptCache);

        assertEquals("Unspecified Cause Of Death", actualCauseOfDeath.getName().getName());
    }

    @Test
    public void shouldReturnUnspecifiedCauseOfDeathIfThePatientIsNewAndPatientIsToBeMarkedDead() throws Exception {
        executeDataSet("testDataSets/patientDeathNoteDS.xml");
        Map<String, Concept> conceptCache = omrsConceptLookup.getConceptsConfiguredViaGlobalProperties(Arrays.asList(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH));
        Patient patient = new Patient();

        patient.setDead(true);
        Concept actualCauseOfDeath = mciPatientService.getCauseOfDeath(patient, conceptCache);

        assertEquals("Unspecified Cause Of Death", actualCauseOfDeath.getName().getName());
    }

    @Test
    public void shouldReturnCauseOfDeathIfCauseOfDeathAttributeIsOtherThanUnspecifiedCauseOfDeath() throws Exception {
        executeDataSet("testDataSets/patientDeathNoteDS.xml");
        Map<String, Concept> conceptCache = omrsConceptLookup.getConceptsConfiguredViaGlobalProperties(Arrays.asList(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH));
        Patient patient = patientService.getPatient(3);

        Concept actualCauseOfDeath = mciPatientService.getCauseOfDeath(patient, conceptCache);

        assertEquals("CANCER", actualCauseOfDeath.getName().getName());


    }

    @Test
    public void shouldThrowRunTimeExceptionIfUnspecifiedCauseOfDeathConceptNotConfiguredInGlobalSettings() throws Exception {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Invalid configuration for Global Setting 'concept.unspecifiedCauseOfDeath',associate Unspecified Cause Of Death concept id to it.");

        Patient patient = new Patient();
        patient.setDead(true);
        mciPatientService.getCauseOfDeath(patient, new HashMap<String, Concept>() {{
            put(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, new Concept());
        }});
    }

    @Test
    public void shouldThrowRunTimeExceptionIfCauseOfDeathConceptNotConfiguredInGlobalSettings() throws Exception {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Invalid configuration for Global Setting 'concept.causeOfDeath',associate Cause Of Death concept id to it.");

        Patient patient = new Patient();
        patient.setDead(true);
        mciPatientService.getCauseOfDeath(patient, new HashMap<String, Concept>() {{
            put(GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH, new Concept());
        }});
    }

    private List<EncounterBundle> getEncounterBundles(String healthId, String shrEncounterId, String encounterBundleFilePath) throws Exception {
        List<EncounterBundle> bundles = new ArrayList<>();
        EncounterBundle bundle = new EncounterBundle();
        bundle.setEncounterId(shrEncounterId);
        bundle.setPublishedDate(new Date().toString());
        bundle.setHealthId(healthId);
        bundle.setLink("http://shr.com/patients/" + healthId + "/encounters/" + shrEncounterId);
        bundle.setTitle("Encounter:" + shrEncounterId);
        bundle.addContent(loadSampleFHIREncounter(encounterBundleFilePath, springContext).getFeed());
        bundles.add(bundle);
        return bundles;
    }

    private ParserBase.ResourceOrFeed loadSampleFHIREncounter(String filePath, ApplicationContext springContext) throws Exception {
        org.springframework.core.io.Resource resource = springContext.getResource(filePath);
        ParserBase.ResourceOrFeed parsedResource =
                new XmlParser().parseGeneral(resource.getInputStream());
        return parsedResource;
    }

}
