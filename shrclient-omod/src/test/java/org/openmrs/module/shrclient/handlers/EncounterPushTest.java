package org.openmrs.module.shrclient.handlers;

import org.hl7.fhir.instance.model.AtomFeed;
import org.ict4h.atomfeed.client.domain.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.UserService;
import org.openmrs.module.fhir.mapper.bundler.CompositionBundle;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SHRClient;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    private UserService userService;

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
                encounterService, userService,
                propertiesReader, compositionBundle,
                idMappingsRepository,
                clientRegistry);
    }

    @Test
    public void shouldProcessEncounterSyncEvent() throws IOException {
        final String uuid = "123abc456";
        String facilityId = "10000069";

        final Event event = new Event("id100", "/openmrs/ws/rest/v1/encounter/" + uuid
                + "?v=custom:(uuid,encounterType,patient,visit,orders:(uuid,orderType,concept,voided))");
        org.openmrs.Encounter openMrsEncounter = getOpenMrsEncounter();
        final AtomFeed atomFeed = new AtomFeed();

        when(propertiesReader.getBaseUrls()).thenReturn(getBaseUrls());
        when(propertiesReader.getShrProperties()).thenReturn(getShrProperties(facilityId));
        when(propertiesReader.getShrPatientEncPathPattern()).thenReturn("/patients/%s/encounters");
        when(propertiesReader.getShrBaseUrl()).thenReturn("http://172.18.46.54:8080");


        when(encounterService.getEncounterByUuid(uuid)).thenReturn(openMrsEncounter);
        when(userService.getUserByUsername(Constants.SHR_CLIENT_SYSTEM_NAME)).thenReturn(new User(2));
        when(shrClient.post(anyString(), eq(atomFeed))).thenReturn("{\"encounterId\":\"shr-uuid\"}");
        when(compositionBundle.create(any(Encounter.class), any(SystemProperties.class))).thenReturn(atomFeed);
        when(idMappingsRepository.findByExternalId(facilityId)).thenReturn(null);
        encounterPush.process(event);

        verify(encounterService).getEncounterByUuid(uuid);
        verify(shrClient).post(anyString(), eq(atomFeed));
        verify(idMappingsRepository).saveMapping(Matchers.<IdMapping>anyObject());
    }

    private org.openmrs.Encounter getOpenMrsEncounter() {
        org.openmrs.Encounter openMrsEncounter = new org.openmrs.Encounter();
        openMrsEncounter.setCreator(new User(1));
        final Patient patient = new Patient();
        openMrsEncounter.setPatient(patient);
        final PersonAttributeType personAttributeType = new PersonAttributeType();
        personAttributeType.setName(Constants.HEALTH_ID_ATTRIBUTE);
        patient.setAttributes(new HashSet<PersonAttribute>(Arrays.asList(new PersonAttribute(personAttributeType, "1234567890123"))));
        return openMrsEncounter;
    }

    @Test
    public void shouldGetEncounterUuidFromEventContent() {
        final String uuid = "123abc456";
        final String content = "/openmrs/ws/rest/v1/encounter/" + uuid +
                "?v=custom:(uuid,encounterType,patient,visit,orders:(uuid,orderType,concept,voided))";
        assertEquals(uuid, encounterPush.getUuid(content));
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
