package org.openmrs.module.shrclient.service.impl;

import org.hl7.fhir.instance.model.AtomEntry;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Composition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir.mapper.emr.FHIRMapper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.ConceptCache;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SystemUserService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.fhir.mapper.MRSProperties.GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH;
import static org.openmrs.module.fhir.mapper.MRSProperties.GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH;

public class MciPatientServiceImplTest {
    @Mock
    private FHIRMapper mockFhirmapper;
    @Mock
    private IdMappingsRepository mockIdMappingsRepository;
    @Mock
    private VisitService mockVisitService;
    @Mock
    private SystemUserService mockSystemUserService;
    @Mock
    private PropertiesReader mockPropertiesReader;
    @Mock
    private ConceptCache conceptCache;
    
    private MciPatientServiceImpl mciPatientService;
    
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mciPatientService = new MciPatientServiceImpl(null, mockVisitService, mockFhirmapper, null, null,
                null, mockIdMappingsRepository, mockPropertiesReader, mockSystemUserService, null, conceptCache);
    }

    @Test
    public void shouldNotSyncAlreadyProcessedEncounter() throws Exception {
        EncounterBundle encounterBundle = new EncounterBundle();
        AtomFeed feed = new AtomFeed();
        Composition composition = new Composition();
        AtomEntry atomEntry = new AtomEntry();
        atomEntry.setResource(composition);
        feed.addEntry(atomEntry);
        encounterBundle.addContent(feed);
        Patient emrPatient = new Patient();

        when(mockIdMappingsRepository.findByExternalId(any(String.class))).thenReturn(new IdMapping());
        mciPatientService.createOrUpdateEncounter(emrPatient, encounterBundle, "health_id");

        verify(mockFhirmapper, times(0)).map(emrPatient, feed);
    }

    @Test
    public void shouldNotSyncConfidentialEncounter() throws Exception {
        EncounterBundle encounterBundle = new EncounterBundle();
        AtomFeed feed = new AtomFeed();
        Composition composition = new Composition();
        composition.setConfidentiality(new Coding().setCodeSimple("R"));
        AtomEntry atomEntry = new AtomEntry();
        atomEntry.setResource(composition);
        feed.addEntry(atomEntry);
        encounterBundle.addContent(feed);
        Patient emrPatient = new Patient();

        when(mockIdMappingsRepository.findByExternalId(any(String.class))).thenReturn(null);

        mciPatientService.createOrUpdateEncounter(emrPatient, encounterBundle, "health_id");

        verify(mockFhirmapper, times(0)).map(emrPatient, feed);
    }

    @Test
    public void shouldSyncNonConfidentialEncounter() throws Exception {
        EncounterBundle encounterBundle = new EncounterBundle();
        AtomFeed feed = new AtomFeed();
        Composition composition = new Composition();
        composition.setConfidentiality(new Coding().setCodeSimple("N"));
        AtomEntry atomEntry = new AtomEntry();
        atomEntry.setResource(composition);
        feed.addEntry(atomEntry);
        encounterBundle.addContent(feed);
        encounterBundle.setTitle("Encounter:shr_encounter_id");
        Patient emrPatient = new Patient();

        when(mockIdMappingsRepository.findByExternalId(any(String.class))).thenReturn(null);
        when(mockFhirmapper.map(emrPatient, feed)).thenReturn(new Encounter());
        when(mockPropertiesReader.getShrBaseUrl()).thenReturn("http://shr.com/");

        mciPatientService.createOrUpdateEncounter(emrPatient, encounterBundle, "health_id");

        verify(mockFhirmapper, times(1)).map(emrPatient, feed);
    }

    @Test
    public void shouldThrowRunTimeExceptionIfUnspecifiedCauseOfDeathConceptNotConfiguredInGlobalSettings() throws Exception {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Invalid configuration for Global Setting 'concept.unspecifiedCauseOfDeath',associate Unspecified Cause Of Death concept id to it.");
        
        when(conceptCache.getConceptFromGlobalProperty(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH)).thenReturn(new Concept());
        when(conceptCache.getConceptFromGlobalProperty(GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH)).thenReturn(null);

        Patient patient = new Patient();
        patient.setDead(true);
        
        mciPatientService.getCauseOfDeath(patient);
    }

    @Test
    public void shouldThrowRunTimeExceptionIfCauseOfDeathConceptNotConfiguredInGlobalSettings() throws Exception {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Invalid configuration for Global Setting 'concept.causeOfDeath',associate Cause Of Death concept id to it.");

        when(conceptCache.getConceptFromGlobalProperty(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH)).thenReturn(null);
        when(conceptCache.getConceptFromGlobalProperty(GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH)).thenReturn(new Concept());

        Patient patient = new Patient();
        patient.setDead(true);
        mciPatientService.getCauseOfDeath(patient);
    }
}