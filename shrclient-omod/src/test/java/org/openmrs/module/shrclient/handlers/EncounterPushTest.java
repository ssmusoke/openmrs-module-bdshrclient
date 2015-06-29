package org.openmrs.module.shrclient.handlers;

import org.hl7.fhir.instance.model.AtomFeed;
import org.ict4h.atomfeed.client.domain.Event;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.module.fhir.mapper.bundler.CompositionBundle;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.util.SystemUserService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SHRClient;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.fhir.utils.PropertyKeyConstants.FACILITY_ID;

public class EncounterPushTest {

    @Mock
    private EncounterService encounterService;

    @Mock
    private SHRClient shrClient;

    @Mock
    private PropertiesReader propertiesReader;

    @Mock
    private SystemUserService systemUserService;

    @Mock
    private CompositionBundle compositionBundle;

    @Mock
    private IdMappingsRepository idMappingsRepository;

    @Mock
    private ClientRegistry clientRegistry;

    private EncounterPush encounterPush;

    @Before
    public void setup() throws IdentityUnauthorizedException {
        initMocks(this);
        when(clientRegistry.getSHRClient()).thenReturn(shrClient);
        encounterPush = new EncounterPush(
                encounterService,
                propertiesReader, compositionBundle,
                idMappingsRepository,
                clientRegistry, systemUserService);
    }

    @Test
    public void shouldProcessEncounterCreateEvent() throws IOException {
        final String uuid = "123abc456";
        String facilityId = "10000069";

        final Event event = new Event("id100", "/openmrs/ws/rest/v1/encounter/" + uuid
                + "?v=custom:(uuid,encounterType,patient,visit,orders:(uuid,orderType,concept,voided))");
        org.openmrs.Encounter openMrsEncounter = getOpenMrsEncounter(uuid);
        final AtomFeed atomFeed = new AtomFeed();

        when(propertiesReader.getBaseUrls()).thenReturn(getBaseUrls());
        when(propertiesReader.getShrProperties()).thenReturn(getShrProperties(facilityId));
        when(propertiesReader.getShrPatientEncPathPattern()).thenReturn("/patients/%s/encounters");
        when(propertiesReader.getShrBaseUrl()).thenReturn("http://172.18.46.54:8080");


        when(encounterService.getEncounterByUuid(uuid)).thenReturn(openMrsEncounter);
        when(shrClient.post(anyString(), eq(atomFeed))).thenReturn("{\"encounterId\":\"shr-uuid\"}");
        when(compositionBundle.create(any(Encounter.class), any(SystemProperties.class))).thenReturn(atomFeed);
        when(idMappingsRepository.findByExternalId(facilityId)).thenReturn(null);

        encounterPush.process(event);

        verify(encounterService).getEncounterByUuid(uuid);
        verify(shrClient).post("patients/1234567890123/encounters", atomFeed);
        ArgumentCaptor<IdMapping> idMappingArgumentCaptor = ArgumentCaptor.forClass(IdMapping.class);
        verify(idMappingsRepository).saveMapping(idMappingArgumentCaptor.capture());

        IdMapping idmapping = idMappingArgumentCaptor.getValue();
        assertEquals("shr-uuid", idmapping.getExternalId());
        assertEquals("shr-uuid", idmapping.getExternalId());
        assertEquals("encounter", idmapping.getType());
        assertEquals("http://172.18.46.54:8080/patients/1234567890123/encounters/shr-uuid", idmapping.getUri());
    }

    @Test
    public void shouldProcessEncounterUpdateEvent() throws Exception {
        final String uuid = "123abc456";

        final Event event = new Event("id100", "/openmrs/ws/rest/v1/encounter/" + uuid
                + "?v=custom:(uuid,encounterType,patient,visit,orders:(uuid,orderType,concept,voided))");
        org.openmrs.Encounter openMrsEncounter = getOpenMrsEncounter(uuid);
        DateTime dateCreated = new DateTime(openMrsEncounter.getDateCreated());
        openMrsEncounter.setDateChanged(dateCreated.plusMinutes(10).toDate());
        final AtomFeed atomFeed = new AtomFeed();

        when(propertiesReader.getShrPatientEncPathPattern()).thenReturn("/patients/%s/encounters");
        when(propertiesReader.getShrBaseUrl()).thenReturn("http://172.18.46.54:8080");
        when(encounterService.getEncounterByUuid(uuid)).thenReturn(openMrsEncounter);
        when(idMappingsRepository.findByInternalId(uuid)).thenReturn(new IdMapping(uuid, "shr-uuid","encounter",null, openMrsEncounter.getDateCreated()));
        when(compositionBundle.create(any(Encounter.class), any(SystemProperties.class))).thenReturn(atomFeed);

        encounterPush.process(event);

        verify(shrClient).put("patients/1234567890123/encounters/shr-uuid", atomFeed);
        verify(idMappingsRepository,times(1)).saveMapping(any(IdMapping.class));
    }

    @Test
    public void shouldNotSyncEncounterIfUpdatedBeforeLastSync() throws Exception {
        final String uuid = "123abc456";

        final Event event = new Event("id100", "/openmrs/ws/rest/v1/encounter/" + uuid
                + "?v=custom:(uuid,encounterType,patient,visit,orders:(uuid,orderType,concept,voided))");
        org.openmrs.Encounter openMrsEncounter = getOpenMrsEncounter(uuid);
        DateTime dateCreated = new DateTime(new Date()).minusMinutes(20);
        openMrsEncounter.setDateCreated(dateCreated.toDate());
        openMrsEncounter.setDateChanged(dateCreated.plusMinutes(10).toDate());
        Date lastSyncDateTime = dateCreated.plusMinutes(15).toDate();
        final AtomFeed atomFeed = new AtomFeed();

        when(propertiesReader.getShrPatientEncPathPattern()).thenReturn("/patients/%s/encounters");
        when(propertiesReader.getShrBaseUrl()).thenReturn("http://172.18.46.54:8080");
        when(encounterService.getEncounterByUuid(uuid)).thenReturn(openMrsEncounter);
        when(idMappingsRepository.findByInternalId(uuid)).thenReturn(new IdMapping(uuid, "shr-uuid","encounter",null, lastSyncDateTime));
        when(compositionBundle.create(any(Encounter.class), any(SystemProperties.class))).thenReturn(atomFeed);

        encounterPush.process(event);

        verify(shrClient, never()).put("patients/1234567890123/encounters/shr-uuid", atomFeed);
    }

    @Test
    public void shouldUpdateEncounterIfUpdatedAfterLastSync() throws Exception {
        final String uuid = "123abc456";

        final Event event = new Event("id100", "/openmrs/ws/rest/v1/encounter/" + uuid
                + "?v=custom:(uuid,encounterType,patient,visit,orders:(uuid,orderType,concept,voided))");
        org.openmrs.Encounter openMrsEncounter = getOpenMrsEncounter(uuid);
        DateTime dateCreated = new DateTime(new Date()).minusMinutes(20);
        openMrsEncounter.setDateCreated(dateCreated.toDate());
        openMrsEncounter.setDateChanged(dateCreated.plusMinutes(10).toDate());
        Date lastSyncDateTime = dateCreated.plusMinutes(5).toDate();
        final AtomFeed atomFeed = new AtomFeed();

        when(propertiesReader.getShrPatientEncPathPattern()).thenReturn("/patients/%s/encounters");
        when(propertiesReader.getShrBaseUrl()).thenReturn("http://172.18.46.54:8080");
        when(encounterService.getEncounterByUuid(uuid)).thenReturn(openMrsEncounter);
        when(idMappingsRepository.findByInternalId(uuid)).thenReturn(new IdMapping(uuid, "shr-uuid","encounter",null, lastSyncDateTime));
        when(compositionBundle.create(any(Encounter.class), any(SystemProperties.class))).thenReturn(atomFeed);

        encounterPush.process(event);

        verify(shrClient, times(1)).put("patients/1234567890123/encounters/shr-uuid", atomFeed);
        verify(idMappingsRepository,times(1)).saveMapping(any(IdMapping.class));
    }

    @Test
    public void shouldNotProcessEncounterDownloadEvent() throws Exception {
        final String uuid = "123abc456";

        final Event event = new Event("id100", "/openmrs/ws/rest/v1/encounter/" + uuid
                + "?v=custom:(uuid,encounterType,patient,visit,orders:(uuid,orderType,concept,voided))");
        org.openmrs.Encounter openMrsEncounter = getOpenMrsEncounter(uuid);
        final AtomFeed atomFeed = new AtomFeed();

        when(propertiesReader.getShrPatientEncPathPattern()).thenReturn("/patients/%s/encounters");

        when(encounterService.getEncounterByUuid(uuid)).thenReturn(openMrsEncounter);
        when(idMappingsRepository.findByInternalId(uuid)).thenReturn(new IdMapping(uuid, "shr-uuid","encounter",null));
        when(compositionBundle.create(any(Encounter.class), any(SystemProperties.class))).thenReturn(atomFeed);
        when(systemUserService.isUpdatedByOpenMRSShrSystemUser(openMrsEncounter)).thenReturn(true);

        encounterPush.process(event);

        verify(shrClient, never()).put("patients/1234567890123/encounters/shr-uuid", atomFeed);
        verify(shrClient, never()).post("patients/1234567890123/encounters/shr-uuid", atomFeed);
        verify(idMappingsRepository,never()).saveMapping(any(IdMapping.class));
    }

    @Test
    public void shouldGetEncounterUuidFromEventContent() {
        final String uuid = "123abc456";
        final String content = "/openmrs/ws/rest/v1/encounter/" + uuid +
                "?v=custom:(uuid,encounterType,patient,visit,orders:(uuid,orderType,concept,voided))";
        assertEquals(uuid, encounterPush.getUuid(content));
    }

    private org.openmrs.Encounter getOpenMrsEncounter(String uuid) {
        org.openmrs.Encounter openMrsEncounter = new org.openmrs.Encounter();
        openMrsEncounter.setUuid(uuid);
        openMrsEncounter.setCreator(new User(1));
        openMrsEncounter.setDateCreated(new Date());
        final Patient patient = new Patient();
        openMrsEncounter.setPatient(patient);
        final PersonAttributeType personAttributeType = new PersonAttributeType();
        personAttributeType.setName(Constants.HEALTH_ID_ATTRIBUTE);
        patient.setAttributes(new HashSet<>(Arrays.asList(new PersonAttribute(personAttributeType, "1234567890123"))));
        return openMrsEncounter;
    }

    private Properties getShrProperties(String facilityId) {
        Properties shrProperties = new Properties();
        shrProperties.setProperty(FACILITY_ID, facilityId);
        return shrProperties;
    }

    private HashMap<String, String> getBaseUrls() {
        HashMap<String, String> baseUrls = new HashMap<>();
        baseUrls.put("mci", "http://mci");
        return baseUrls;
    }
}
