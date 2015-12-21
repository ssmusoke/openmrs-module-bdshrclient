package org.openmrs.module.shrclient.feeds.shr;

import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import com.sun.syndication.feed.atom.Category;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.shrclient.handlers.ClientRegistry;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.service.EMREncounterService;
import org.openmrs.module.shrclient.service.EMRPatientService;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.web.controller.dto.EncounterEvent;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.shrclient.web.controller.dto.EncounterEvent.ENCOUNTER_UPDATED_CATEGORY_TAG;

public class DefaultEncounterFeedWorkerTest {

    @Mock
    private EMRPatientService emrPatientService;
    @Mock
    private EMREncounterService emrEncounterService;
    @Mock
    private PropertiesReader propertiesReader;

    @Mock
    private ClientRegistry clientRegistry;
    private DefaultEncounterFeedWorker encounterFeedWorker;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        encounterFeedWorker = new DefaultEncounterFeedWorker(emrPatientService, emrEncounterService, propertiesReader, clientRegistry);
    }

    @Test
    public void shouldSyncConfidentialEncounter() throws Exception {
        EncounterEvent encounterEvent = new EncounterEvent();
        encounterEvent.setTitle("Encounter:shr-enc-id");
        Bundle bundle = new Bundle();
        String healthId = "health_id";
        String mciUrl = "http://mci.com/api/patients/";
        String patientUrl = mciUrl + healthId;
        ResourceReferenceDt patientReference = new ResourceReferenceDt(patientUrl);
        Composition composition = new Composition();
        composition.setSubject(patientReference);
        composition.setConfidentiality("R");
        Bundle.Entry atomEntry = new Bundle.Entry();
        atomEntry.setResource(composition);
        bundle.addEntry(atomEntry);
        String encounterUpdatedDate = DateUtil.toISOString(DateTime.now().toDate());
        Category category = new Category();
        category.setTerm(ENCOUNTER_UPDATED_CATEGORY_TAG + ":" + encounterUpdatedDate);
        encounterEvent.setCategories(asList(category));
        encounterEvent.addContent(bundle);

        when(propertiesReader.getMciPatientContext()).thenReturn(mciUrl);
        RestClient mciClient = mock(RestClient.class);
        when(clientRegistry.getMCIClient()).thenReturn(mciClient);
        Patient patient = new Patient();
        when(mciClient.get(patientUrl, Patient.class)).thenReturn(patient);
        org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
        when(emrPatientService.createOrUpdatePatient(patient)).thenReturn(openmrsPatient);

        encounterFeedWorker.process(encounterEvent);

        verify(mciClient, times(1)).get(patientUrl, Patient.class);
        verify(emrEncounterService, times(1)).createOrUpdateEncounter(openmrsPatient, encounterEvent, healthId);
    }
}