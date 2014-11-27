package org.openmrs.module.shrclient.handlers;

import org.ict4h.atomfeed.client.domain.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.User;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.UserService;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.mapper.PatientMapper;
import org.openmrs.module.shrclient.util.RestClient;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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
    private RestClient restClient;

    private PatientPush patientPush;

    private String healthId = "hid-200";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        patientPush = new PatientPush(patientService, userService, personService, patientMapper, restClient);
    }

    @Test
    public void shouldNotSyncPatientWhenUsersAreSame() {
        final org.openmrs.Patient openMrsPatient = new org.openmrs.Patient();
        User shrUser = new User();
        shrUser.setId(2);
        openMrsPatient.setCreator(shrUser);
        when(userService.getUserByUuid(Constants.OPENMRS_DAEMON_USER)).thenReturn(shrUser);

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
        when(userService.getUserByUuid(Constants.OPENMRS_DAEMON_USER)).thenReturn(shrUser);

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
        healthIdAttributeType.setName(Constants.HEALTH_ID_ATTRIBUTE);

        PersonAttribute healthIdAttribute = new PersonAttribute();
        healthIdAttribute.setAttributeType(healthIdAttributeType);
        healthIdAttribute.setValue(healthId);

        Set<PersonAttribute> openMrsPatientAttributes = new HashSet<PersonAttribute>();
        openMrsPatientAttributes.add(healthIdAttribute);
        openMrsPatient.setAttributes(openMrsPatientAttributes);

        patientPush.updateOpenMrsPatientHealthId(openMrsPatient, healthId);
        verify(patientService, never()).savePatient(any(org.openmrs.Patient.class));
    }

    @Test
    public void shouldUpdateOpenMrsPatient_WhenNewHealthIdIsProvided() {
        final org.openmrs.Patient openMrsPatient = new org.openmrs.Patient();
        PersonAttributeType healthIdAttributeType = new PersonAttributeType();
        healthIdAttributeType.setName(Constants.HEALTH_ID_ATTRIBUTE);
        Set<PersonAttribute> openMrsPatientAttributes = new HashSet<PersonAttribute>();
        openMrsPatient.setAttributes(openMrsPatientAttributes);

        when(personService.getPersonAttributeTypeByName(Constants.HEALTH_ID_ATTRIBUTE)).thenReturn(healthIdAttributeType);
        patientPush.updateOpenMrsPatientHealthId(openMrsPatient, healthId);
        verify(patientService).savePatient(any(org.openmrs.Patient.class));
    }
}
