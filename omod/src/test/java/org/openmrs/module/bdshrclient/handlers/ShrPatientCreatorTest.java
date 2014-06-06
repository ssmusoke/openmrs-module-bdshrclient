package org.openmrs.module.bdshrclient.handlers;

import org.ict4h.atomfeed.client.domain.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.*;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.UserService;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;
import org.openmrs.module.bdshrclient.util.Constants;
import org.openmrs.module.bdshrclient.util.GenderEnum;
import org.openmrs.module.bdshrclient.util.MciProperties;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.bdshrclient.util.Constants.*;

public class ShrPatientCreatorTest {

    @Mock
    private PatientService patientService;
    @Mock
    private AddressHierarchyService addressHierarchyService;
    @Mock
    private UserService userService;
    @Mock
    private PersonService personService;

    @Mock
    Properties properties;

    private ShrPatientCreator shrPatientCreator;

    private String nationalId = "nid-100";
    private String healthId = "hid-200";
    private String occupation = "salaried";
    private String educationLevel = "graduate";
    private String primaryContact = "some contact";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        shrPatientCreator = new ShrPatientCreator(addressHierarchyService, patientService, userService, personService);
    }

    @Test
    public void shouldProcessPatientSyncEvent() {
        final String uuid = "123abc456";
        Event event = new Event("id100", "/openmrs/ws/rest/v1/patient/" + uuid + "?v=full");
        shrPatientCreator.process(event);
        verify(patientService).getPatientByUuid(anyString());
//        verify(userService).getUserByUsername(anyString());
    }

    @Test
    public void shouldNotSyncPatientWhenUsersAreSame() {
        final org.openmrs.Patient openMrsPatient = new org.openmrs.Patient();
        User shrUser = new User();
        shrUser.setId(2);
        openMrsPatient.setCreator(shrUser);
        when(userService.getUserByUsername(SHR_CLIENT_SYSTEM_NAME)).thenReturn(shrUser);

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
        when(userService.getUserByUsername(SHR_CLIENT_SYSTEM_NAME)).thenReturn(shrUser);

        assertTrue(shrPatientCreator.shouldSyncPatient(openMrsPatient));
    }

    @Test
    public void shouldGetMciUrl() throws IOException {
        MciProperties mciProperties = new MciProperties(properties);
        when(properties.getProperty("mci.host")).thenReturn("localhost");
        when(properties.getProperty("mci.port")).thenReturn("8080");
        assertEquals("http://localhost:8080/patient", shrPatientCreator.getMciUrl(mciProperties));
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
        healthIdAttributeType.setName(HEALTH_ID_ATTRIBUTE);

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
        healthIdAttributeType.setName(HEALTH_ID_ATTRIBUTE);
        Set<PersonAttribute> openMrsPatientAttributes = new HashSet<PersonAttribute>();
        openMrsPatient.setAttributes(openMrsPatientAttributes);

        when(personService.getPersonAttributeTypeByName(HEALTH_ID_ATTRIBUTE)).thenReturn(healthIdAttributeType);
        shrPatientCreator.updateOpenMrsPatientHealthId(openMrsPatient, healthId);
        verify(patientService).savePatient(any(org.openmrs.Patient.class));
    }

    @Test
    public void shouldPopulateFreeShrPatientFromOpenMrsPatient() throws Exception {
        final String givenName = "Sachin";
        final String middleName = "Ramesh";
        final String familyName = "Tendulkar";
        final String gender = "M";
        final Date dateOfBirth = new SimpleDateFormat(Constants.ISO_DATE_FORMAT).parse("2000-12-31");

        final String addressLine = "house10";
        final String divisionId = "10";
        final String division = "some-division";
        final String districtId = "1020";
        final String district = "some-district";
        final String upazillaId = "102030";
        final String upazilla = "some-upazilla";
        final String unionId = "10203040";
        final String union = "some-union";

        Person person = new Person();
        PersonName personName = new PersonName(givenName, middleName, familyName);
        person.addName(personName);
        person.setGender(gender);
        person.setBirthdate(dateOfBirth);

        PersonAddress address = new PersonAddress();
        address.setAddress1(addressLine);
        address.setStateProvince(division);
        address.setCountyDistrict(district);
        address.setAddress3(upazilla);
        address.setCityVillage(union);
        person.addAddress(address);
        org.openmrs.Patient openMrsPatient = new org.openmrs.Patient(person);

        openMrsPatient.setAttributes(createOpenMrsPersonAttributes());

        List<AddressHierarchyLevel> levels = createAddressHierarchyLevels();
        List<AddressHierarchyEntry> divisionEntries = createAddressHierarchyEntries(divisionId);
        List<AddressHierarchyEntry> districtEntries = createAddressHierarchyEntries(districtId);
        List<AddressHierarchyEntry> upazillaEntries = createAddressHierarchyEntries(upazillaId);
        List<AddressHierarchyEntry> unionEntries = createAddressHierarchyEntries(unionId);


        when(addressHierarchyService.getAddressHierarchyLevels()).thenReturn(levels);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(0), division)).thenReturn(divisionEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(1), district)).thenReturn(districtEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(2), upazilla)).thenReturn(upazillaEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(3), union)).thenReturn(unionEntries);

        Patient patient = shrPatientCreator.populatePatient(openMrsPatient);

        Patient p = new Patient();
        p.setNationalId(nationalId);
        p.setHealthId(healthId);
        p.setFirstName(givenName);
        p.setMiddleName(middleName);
        p.setLastName(familyName);
        p.setGender(GenderEnum.getCode(gender));
        p.setDateOfBirth(new SimpleDateFormat(Constants.ISO_DATE_FORMAT).format(dateOfBirth));
        p.setOccupation(occupation);
        p.setEducationLevel(educationLevel);
        p.setPrimaryContact(primaryContact);

        Address a = new Address();
        a.setAddressLine(addressLine);
        a.setDivisionId(divisionId);
        a.setDistrictId(districtId);
        a.setUpazillaId(upazillaId);
        a.setUnionId(unionId);
        p.setAddress(a);

        assertEquals(p, patient);
    }

    private Set<PersonAttribute> createOpenMrsPersonAttributes() {
        Set<PersonAttribute> attributes = new HashSet<PersonAttribute>();
        final PersonAttributeType nationalIdAttrType = new PersonAttributeType();
        nationalIdAttrType.setName(NATIONAL_ID_ATTRIBUTE);
        attributes.add(new PersonAttribute(nationalIdAttrType, nationalId));

        final PersonAttributeType healthIdAttrType = new PersonAttributeType();
        healthIdAttrType.setName(HEALTH_ID_ATTRIBUTE);
        attributes.add(new PersonAttribute(healthIdAttrType, healthId));

        final PersonAttributeType occupationAttrType = new PersonAttributeType();
        occupationAttrType.setName(OCCUPATION_ATTRIBUTE);
        attributes.add(new PersonAttribute(occupationAttrType, occupation));

        final PersonAttributeType educationAttrType = new PersonAttributeType();
        educationAttrType.setName(EDUCATION_ATTRIBUTE);
        attributes.add(new PersonAttribute(educationAttrType, educationLevel));

        final PersonAttributeType primaryContactAttrType = new PersonAttributeType();
        primaryContactAttrType.setName(PRIMARY_CONTACT_ATTRIBUTE);
        attributes.add(new PersonAttribute(primaryContactAttrType, primaryContact));
        return attributes;
    }

    private List<AddressHierarchyLevel> createAddressHierarchyLevels() {
        AddressHierarchyLevel level1 = new AddressHierarchyLevel();
        level1.setId(100);
        AddressHierarchyLevel level2 = new AddressHierarchyLevel();
        level2.setId(200);
        AddressHierarchyLevel level3 = new AddressHierarchyLevel();
        level3.setId(300);
        AddressHierarchyLevel level4 = new AddressHierarchyLevel();
        level4.setId(400);
        return Arrays.asList(level1, level2, level3, level4);
    }

    private List<AddressHierarchyEntry> createAddressHierarchyEntries(String id) {
        List<AddressHierarchyEntry> entries1 = new ArrayList<AddressHierarchyEntry>();
        AddressHierarchyEntry entry1 = new AddressHierarchyEntry();
        entry1.setUserGeneratedId(id);
        entries1.add(entry1);
        return entries1;
    }

    @Test
    public void shouldHttpPostPatientToUrl() {
        //TODO
    }
}
