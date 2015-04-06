package org.openmrs.module.shrclient.handlers;

import org.ict4h.atomfeed.client.domain.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.User;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.UserService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.mapper.PatientMapper;
import org.openmrs.module.shrclient.util.PropertiesReader;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.fhir.utils.Constants.HEALTH_ID_ATTRIBUTE;
import static org.openmrs.module.fhir.utils.Constants.OPENMRS_DAEMON_USER;

public class PatientPushTest {

    @Mock
    private PatientService patientService;
    @Mock
    private UserService userService;
    @Mock
    private PersonService personService;
    @Mock
    private PatientMapper patientMapper;

    @Mock
    private PropertiesReader propertiesReader;
    @Mock
    private IdMappingsRepository idMappingsRepository;

    @Mock
    private ClientRegistry clientRegistry;
    private PatientPush patientPush;

    private String healthId = "hid-200";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        patientPush = new PatientPush(patientService, userService, personService, patientMapper, propertiesReader, clientRegistry, idMappingsRepository);
    }

    @Test
    public void shouldNotSyncPatientWhenUsersAreSame() {
        final org.openmrs.Patient openMrsPatient = new org.openmrs.Patient();
        User shrUser = new User();
        shrUser.setId(2);
        openMrsPatient.setCreator(shrUser);
        when(userService.getUserByUuid(OPENMRS_DAEMON_USER)).thenReturn(shrUser);

        assertFalse(patientPush.isUpdatedByEmrUser(openMrsPatient));
    }

    @Test
    public void shouldSyncPatientWhenUsersAreDifferent() {
        final org.openmrs.Patient openMrsPatient = new org.openmrs.Patient();
        User bahmniUser = new User();
        bahmniUser.setId(1);
        User shrUser = new User();
        shrUser.setId(2);
        openMrsPatient.setCreator(bahmniUser);
        when(userService.getUserByUuid(OPENMRS_DAEMON_USER)).thenReturn(shrUser);

        assertTrue(patientPush.isUpdatedByEmrUser(openMrsPatient));
    }

    @Test
    public void shouldGetPatientUuidFromEvent() {
        final String uuid = "123abc456";
        Event event = new Event("id100", "/openmrs/ws/rest/v1/patient/" + uuid + "?v=full");
        assertEquals(uuid, patientPush.getPatientUuid(event));
    }

    @Test
    public void shouldNotUpdateOpenMrsPatient_WhenHealthIdIsBlankOrNull() {
        patientPush.updateOpenMrsPatientHealthId(new org.openmrs.Patient(), " ");
        verify(patientService, never()).savePatient(any(org.openmrs.Patient.class));
        patientPush.updateOpenMrsPatientHealthId(new org.openmrs.Patient(), null);
        verify(patientService, never()).savePatient(any(org.openmrs.Patient.class));
    }

    @Test
    public void shouldNotUpdateOpenMrsPatient_WhenHealthIdAttributeIsSameAsProvidedHealthId() {
        final org.openmrs.Patient openMrsPatient = new org.openmrs.Patient();

        PersonAttributeType healthIdAttributeType = new PersonAttributeType();
        healthIdAttributeType.setName(HEALTH_ID_ATTRIBUTE);

        PersonAttribute healthIdAttribute = new PersonAttribute();
        healthIdAttribute.setAttributeType(healthIdAttributeType);
        healthIdAttribute.setValue(healthId);

        Set<PersonAttribute> openMrsPatientAttributes = new HashSet<>();
        openMrsPatientAttributes.add(healthIdAttribute);
        openMrsPatient.setAttributes(openMrsPatientAttributes);

        patientPush.updateOpenMrsPatientHealthId(openMrsPatient, healthId);
        verify(patientService, never()).savePatient(any(org.openmrs.Patient.class));
    }

    @Test
    public void shouldUpdateOpenMrsPatient_WhenNewHealthIdIsProvided() {
        final org.openmrs.Patient openMrsPatient = new org.openmrs.Patient();
        PersonAttributeType healthIdAttributeType = new PersonAttributeType();
        healthIdAttributeType.setName(HEALTH_ID_ATTRIBUTE);
        Set<PersonAttribute> openMrsPatientAttributes = new HashSet<>();
        openMrsPatient.setAttributes(openMrsPatientAttributes);

        when(personService.getPersonAttributeTypeByName(HEALTH_ID_ATTRIBUTE)).thenReturn(healthIdAttributeType);
        when(propertiesReader.getBaseUrls()).thenReturn(new HashMap<String, String>() {{
            put("mci", "http://example.org");
        }});
        patientPush.updateOpenMrsPatientHealthId(openMrsPatient, healthId);
        verify(patientService).savePatient(any(org.openmrs.Patient.class));
    }

    @Test
    public void shouldNotProcessIfPatientHasBeenChangedAfterEvent() throws Exception {
        String content = "/openmrs/ws/rest/v1/patient/36c82d16-6237-4495-889f-59bd9e0d8181?v=full";
        Date eventUpdatedDate = new Date();
        Event event = new Event("123defc456", content, "Patient", null, eventUpdatedDate);
        Patient openMrsPatient = new Patient();
        Calendar instance = Calendar.getInstance();
        instance.setTime(eventUpdatedDate);
        instance.add(Calendar.MINUTE, 2);
        openMrsPatient.setDateChanged(instance.getTime());
        when(patientService.getPatientByUuid("36c82d16-6237-4495-889f-59bd9e0d8181")).thenReturn(openMrsPatient);

        patientPush.process(event);

        verify(patientMapper, never()).map(openMrsPatient);
    }
}
