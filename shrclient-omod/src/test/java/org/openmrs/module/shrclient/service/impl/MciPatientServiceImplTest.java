package org.openmrs.module.shrclient.service.impl;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir.mapper.emr.FHIRMapper;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SystemUserService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.fhir.mapper.MRSProperties.*;

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
    private GlobalPropertyLookUpService mockGlobalPropertyLookUpService;
    @Mock
    private ConceptService mockConceptService;

    private MciPatientServiceImpl mciPatientService;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mciPatientService = new MciPatientServiceImpl(null, mockVisitService, mockFhirmapper, null, null,
                null, mockIdMappingsRepository, mockPropertiesReader, mockSystemUserService, null, mockConceptService, mockGlobalPropertyLookUpService);
    }

    @Test
    public void shouldNotSyncAlreadyProcessedEncounter() throws Exception {
        EncounterBundle encounterBundle = new EncounterBundle();
        Bundle bundle = new Bundle();
        Composition composition = new Composition();
        Bundle.Entry atomEntry = new Bundle.Entry();
        atomEntry.setResource(composition);
        bundle.addEntry(atomEntry);
        encounterBundle.addContent(bundle);
        Patient emrPatient = new Patient();

        when(mockIdMappingsRepository.findByExternalId(any(String.class))).thenReturn(new IdMapping());
        mciPatientService.createOrUpdateEncounter(emrPatient, encounterBundle, "health_id");

        verify(mockFhirmapper, times(0)).map(emrPatient, bundle);
    }

    @Test
    public void shouldNotSyncConfidentialEncounter() throws Exception {
        EncounterBundle encounterBundle = new EncounterBundle();
        Bundle bundle = new Bundle();
        Composition composition = new Composition();
        composition.setConfidentiality("R");
        Bundle.Entry atomEntry = new Bundle.Entry();
        atomEntry.setResource(composition);
        bundle.addEntry(atomEntry);
        encounterBundle.addContent(bundle);
        Patient emrPatient = new Patient();

        when(mockIdMappingsRepository.findByExternalId(any(String.class))).thenReturn(null);

        mciPatientService.createOrUpdateEncounter(emrPatient, encounterBundle, "health_id");

        verify(mockFhirmapper, times(0)).map(emrPatient, bundle);
    }

    @Test
    public void shouldSyncNonConfidentialEncounter() throws Exception {
        EncounterBundle encounterBundle = new EncounterBundle();
        Bundle bundle = new Bundle();
        Composition composition = new Composition();
        composition.setConfidentiality("N");
        Bundle.Entry atomEntry = new Bundle.Entry();
        atomEntry.setResource(composition);
        bundle.addEntry(atomEntry);
        encounterBundle.addContent(bundle);
        encounterBundle.setTitle("Encounter:shr_encounter_id");
        Patient emrPatient = new Patient();

        when(mockIdMappingsRepository.findByExternalId(any(String.class))).thenReturn(null);
        when(mockFhirmapper.map(emrPatient, bundle)).thenReturn(new Encounter());
        when(mockPropertiesReader.getShrBaseUrl()).thenReturn("http://shr.com/");

        mciPatientService.createOrUpdateEncounter(emrPatient, encounterBundle, "health_id");

        verify(mockFhirmapper, times(1)).map(emrPatient, bundle);
    }

    @Test
    public void shouldThrowRunTimeExceptionIfUnspecifiedCauseOfDeathConceptNotConfiguredInGlobalSettings() throws Exception {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage(String.format("Global Property %s is not set & Concept with name %s is not found", GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH, TR_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH));

        Concept causeOfDeathConcept = new Concept(1001);
        when(mockGlobalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH)).thenReturn("1001");
        when(mockConceptService.getConcept(causeOfDeathConcept.getId())).thenReturn(causeOfDeathConcept);
        when(mockGlobalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH)).thenReturn(null);

        Patient patient = new Patient();
        patient.setDead(true);

        mciPatientService.getCauseOfDeath(patient);
    }

    @Test
    public void shouldThrowRunTimeExceptionIfCauseOfDeathConceptNotConfiguredInGlobalSettings() throws Exception {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage(String.format("Global Property %s is not set & Concept with name %s is not found", GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, TR_CONCEPT_CAUSE_OF_DEATH));

        Concept unspecifiedCauseOfDeathConcept = new Concept(1002);
        when(mockGlobalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH)).thenReturn(null);
        when(mockGlobalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH)).thenReturn("1002");
        when(mockConceptService.getConcept(unspecifiedCauseOfDeathConcept.getId())).thenReturn(unspecifiedCauseOfDeathConcept);

        Patient patient = new Patient();
        patient.setDead(true);
        mciPatientService.getCauseOfDeath(patient);
    }
}