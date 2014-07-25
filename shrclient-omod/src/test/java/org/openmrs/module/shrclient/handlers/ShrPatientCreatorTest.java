package org.openmrs.module.shrclient.handlers;

import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.mapper.PatientMapper;
import org.openmrs.module.shrclient.util.RestClient;
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

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ShrPatientCreatorTest {

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

    private ShrPatientCreator shrPatientCreator;

    private String healthId = "hid-200";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        shrPatientCreator = new ShrPatientCreator(patientService, userService, personService, patientMapper, restClient);
    }

    @Test
    public void shouldProcessPatientSyncEvent() {
        final String uuid = "123abc456";
        Event event = new Event("id100", "/openmrs/ws/rest/v1/patient/" + uuid + "?v=full");

//        final org.openmrs.Patient openMrsPatient = new org.openmrs.Patient();
//        final org.bahmni.module.shrclient.model.Patient patient = new org.bahmni.module.shrclient.model.Patient();
//        when(patientService.getPatientByUuid(uuid)).thenReturn(openMrsPatient);
//        when(patientMapper.map(openMrsPatient)).thenReturn(patient);
//        when(userService.getUserByUsername(Constants.SHR_CLIENT_SYSTEM_NAME)).thenReturn(new User());
//        shrPatientCreator.process(event);
//
//        verify(patientService).getPatientByUuid(uuid);
//        verify(patientMapper).map(openMrsPatient);
//        verify(restClient).post(anyString(), eq(patient));

        shrPatientCreator.process(event);
        verify(patientService).getPatientByUuid(anyString());
    }

    @Test
    public void shouldNotSyncPatientWhenUsersAreSame() {
        final org.openmrs.Patient openMrsPatient = new org.openmrs.Patient();
        User shrUser = new User();
        shrUser.setId(2);
        openMrsPatient.setCreator(shrUser);
        when(userService.getUserByUsername(Constants.SHR_CLIENT_SYSTEM_NAME)).thenReturn(shrUser);

        assertFalse(shrPatientCreator.shouldSyncPatient(openMrsPatient));
    }

    @Test
    public void shouldSyncPatientWhenUsersAreDifferent() {
        final org.openmrs.Patient openMrsPatient = new org.openmrs.Patient();
        User bahmniUser = new User();
        bahmniUser.setId(1);
        User shrUser = new User();
        shrUser.setId(2);
        openMrsPatient.setCreator(bahmniUser);
        when(userService.getUserByUsername(Constants.SHR_CLIENT_SYSTEM_NAME)).thenReturn(shrUser);

        assertTrue(shrPatientCreator.shouldSyncPatient(openMrsPatient));
    }

    @Test
    public void shouldGetPatientUuidFromEvent() {
        final String uuid = "123abc456";
        Event event = new Event("id100", "/openmrs/ws/rest/v1/patient/" + uuid + "?v=full");
        assertEquals(uuid, shrPatientCreator.getPatientUuid(event));
    }

    @Test
    public void shouldNotUpdateOpenMrsPatient_WhenHealthIdIsBlankOrNull() {
        shrPatientCreator.updateOpenMrsPatientHealthId(new org.openmrs.Patient(), " ");
        verify(patientService, never()).savePatient(any(org.openmrs.Patient.class));
        shrPatientCreator.updateOpenMrsPatientHealthId(new org.openmrs.Patient(), null);
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

        shrPatientCreator.updateOpenMrsPatientHealthId(openMrsPatient, healthId);
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
        shrPatientCreator.updateOpenMrsPatientHealthId(openMrsPatient, healthId);
        verify(patientService).savePatient(any(org.openmrs.Patient.class));
    }
}
