package org.openmrs.module.shrclient.service.impl;

import org.hl7.fhir.instance.model.AtomEntry;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Composition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir.mapper.emr.FHIRMapper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SystemUserService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

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

    private MciPatientServiceImpl mciPatientService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mciPatientService = new MciPatientServiceImpl(null, mockVisitService, mockFhirmapper, null, null,
                null, mockIdMappingsRepository, mockPropertiesReader, mockSystemUserService, null);
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
        mciPatientService.createOrUpdateEncounter(emrPatient, encounterBundle, "health_id", null);

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

        mciPatientService.createOrUpdateEncounter(emrPatient, encounterBundle, "health_id", null);

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

        mciPatientService.createOrUpdateEncounter(emrPatient, encounterBundle, "health_id", null);

        verify(mockFhirmapper, times(1)).map(emrPatient, feed);
    }
}