package org.openmrs.module.bdshrclient.handlers;

import org.ict4h.atomfeed.client.domain.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.*;
import org.openmrs.api.PatientService;
import org.openmrs.api.UserService;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;
import org.openmrs.module.bdshrclient.util.GenderEnum;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.bdshrclient.handlers.ShrPatientCreator.ISO_DATE_FORMAT;

public class ShrPatientCreatorTest {

    @Mock
    private PatientService patientService;
    @Mock
    private AddressHierarchyService addressHierarchyService;
    @Mock
    private UserService userService;

    private ShrPatientCreator shrPatientCreator;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        shrPatientCreator = new ShrPatientCreator(addressHierarchyService, patientService, userService);
    }


    @Test
    public void shouldCreatePatientFromEvent() throws Exception {
        final String uuid = "123abc456";

        final String nationalId = "nid-100";
        final String healthId = "hid-200";
        final String givenName = "Sachin";
        final String middleName = "Ramesh";
        final String familyName = "Tendulkar";
        final String gender = "M";
        final Date dateOfBirth = new SimpleDateFormat(ISO_DATE_FORMAT).parse("2000-12-31");
        final String occupation = "salaried";
        final String educationLevel = "graduate";
        final String primaryContact = "some contact";

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

        Set<PersonAttribute> attributes = new HashSet<PersonAttribute>();
        final PersonAttributeType nationalIdAttrType = new PersonAttributeType();
        nationalIdAttrType.setName("National ID");
        attributes.add(new PersonAttribute(nationalIdAttrType, nationalId));

        final PersonAttributeType healthIdAttrType = new PersonAttributeType();
        healthIdAttrType.setName("Health ID");
        attributes.add(new PersonAttribute(healthIdAttrType, healthId));

        final PersonAttributeType occupationAttrType = new PersonAttributeType();
        occupationAttrType.setName("occupation");
        attributes.add(new PersonAttribute(occupationAttrType, occupation));

        final PersonAttributeType educationAttrType = new PersonAttributeType();
        educationAttrType.setName("education");
        attributes.add(new PersonAttribute(educationAttrType, educationLevel));

        final PersonAttributeType primaryContactAttrType = new PersonAttributeType();
        primaryContactAttrType.setName("primaryContact");
        attributes.add(new PersonAttribute(primaryContactAttrType, primaryContact));

        openMrsPatient.setAttributes(attributes);

        AddressHierarchyLevel level1 = new AddressHierarchyLevel();
        level1.setId(100);
        AddressHierarchyLevel level2 = new AddressHierarchyLevel();
        level2.setId(200);
        AddressHierarchyLevel level3 = new AddressHierarchyLevel();
        level3.setId(300);
        AddressHierarchyLevel level4 = new AddressHierarchyLevel();
        level4.setId(400);

        List<AddressHierarchyEntry> entries1 = new ArrayList<AddressHierarchyEntry>();
        AddressHierarchyEntry entry1 = new AddressHierarchyEntry();
        entry1.setUserGeneratedId(divisionId);
        entries1.add(entry1);

        List<AddressHierarchyEntry> entries2 = new ArrayList<AddressHierarchyEntry>();
        AddressHierarchyEntry entry2 = new AddressHierarchyEntry();
        entry2.setUserGeneratedId(districtId);
        entries2.add(entry2);

        List<AddressHierarchyEntry> entries3 = new ArrayList<AddressHierarchyEntry>();
        AddressHierarchyEntry entry3 = new AddressHierarchyEntry();
        entry3.setUserGeneratedId(upazillaId);
        entries3.add(entry3);

        List<AddressHierarchyEntry> entries4 = new ArrayList<AddressHierarchyEntry>();
        AddressHierarchyEntry entry4 = new AddressHierarchyEntry();
        entry4.setUserGeneratedId(unionId);
        entries4.add(entry4);


        List<AddressHierarchyLevel> levels = Arrays.asList(level1, level2, level3, level4);

        when(addressHierarchyService.getAddressHierarchyLevels()).thenReturn(levels);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(0), division)).thenReturn(entries1);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(1), district)).thenReturn(entries2);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(2), upazilla)).thenReturn(entries3);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(3), union)).thenReturn(entries4);

        when(patientService.getPatientByUuid(uuid)).thenReturn(openMrsPatient);

        Event event = new Event("id100", "/openmrs/ws/rest/v1/patient/" + uuid + "?v=full");
        String patientUuid = shrPatientCreator.getPatientUuid(event);
        Patient patient = shrPatientCreator.populatePatient(openMrsPatient);

        Patient p = new Patient();
        p.setNationalId(nationalId);
        p.setHealthId(healthId);
        p.setFirstName(givenName);
        p.setMiddleName(middleName);
        p.setLastName(familyName);
        p.setGender(GenderEnum.getCode(gender));
        p.setDateOfBirth(new SimpleDateFormat(ISO_DATE_FORMAT).format(dateOfBirth));
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
}
