package org.bahmni.module.shrclient.handlers;

import org.bahmni.module.shrclient.mapper.DiagnosisMapper;
import org.bahmni.module.shrclient.mapper.EncounterMapper;
import org.bahmni.module.shrclient.util.FhirRestClient;
import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.Encounter;
import org.ict4h.atomfeed.client.domain.Event;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.api.EncounterService;
import org.openmrs.api.UserService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ShrEncounterCreatorTest {

    @Mock
    private EncounterService encounterService;
    @Mock
    private EncounterMapper encounterMapper;

    @Mock
    private DiagnosisMapper diagnosisMapper;

    @Mock
    private FhirRestClient fhirRestClient;

    @Mock
    private UserService userService;

    private ShrEncounterUploader shrEncounterUploader;

    @Before
    public void setup() {
        initMocks(this);
        shrEncounterUploader = new ShrEncounterUploader(encounterService, encounterMapper, diagnosisMapper, fhirRestClient, userService);
    }

    @Ignore
    @Test
    public void shouldProcessEncounterSyncEvent() throws IOException {
        final String uuid = "123abc456";
        final Event event = new Event("id100", "/openmrs/ws/rest/v1/encounter/" + uuid
                + "?v=custom:(uuid,encounterType,patient,visit,orders:(uuid,orderType,concept,voided))");
        final org.openmrs.Encounter openMrsEncounter = new org.openmrs.Encounter();
        final Encounter encounter = new Encounter();
        final List<Condition> conditions = new ArrayList<Condition>();

        when(encounterService.getEncounterByUuid(uuid)).thenReturn(openMrsEncounter);
        when(encounterMapper.map(openMrsEncounter)).thenReturn(encounter);
        when(diagnosisMapper.map(openMrsEncounter, encounter)).thenReturn(conditions);
        shrEncounterUploader.process(event);

        verify(encounterService).getEncounterByUuid(uuid);
        verify(encounterMapper).map(openMrsEncounter);
//        verify(fhirRestClient).post(anyString(), eq(encounter));
    }

    @Test
    public void shouldGetEncounterUuidFromEventContent() {
        final String uuid = "123abc456";
        final String content = "/openmrs/ws/rest/v1/encounter/" + uuid +
                "?v=custom:(uuid,encounterType,patient,visit,orders:(uuid,orderType,concept,voided))";
        assertEquals(uuid, shrEncounterUploader.getUuid(content));
    }

}
