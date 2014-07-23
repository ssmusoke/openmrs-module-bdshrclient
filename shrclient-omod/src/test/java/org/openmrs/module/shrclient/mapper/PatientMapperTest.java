package org.openmrs.module.shrclient.mapper;

import org.openmrs.module.shrclient.model.Address;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.service.BbsCodeService;
import org.openmrs.module.shrclient.service.impl.BbsCodeServiceImpl;
import org.openmrs.module.bahmni.utils.Constants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.*;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PatientMapperTest {

    @Mock
    private AddressHierarchyService addressHierarchyService;
    private BbsCodeService bbsCodeService;
    private PatientMapper patientMapper;

    private String nationalId = "nid-100";
    private String healthId = "hid-200";
    private String occupation = "agriculture";
    private String educationLevel = "6th to 9th";
    private String primaryContact = "some contact";

    @Before
    public void setup() throws Exception {
        initMocks(this);
        this.bbsCodeService = new BbsCodeServiceImpl();
        patientMapper = new PatientMapper(addressHierarchyService, bbsCodeService);
    }

    @Test
    public void shouldOpenMrsPatientToMciPatient() throws Exception {
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

        Patient patient = patientMapper.map(openMrsPatient);

        Patient p = new Patient();
        p.setNationalId(nationalId);
        p.setHealthId(healthId);
        p.setFirstName(givenName);
        p.setMiddleName(middleName);
        p.setLastName(familyName);
        p.setGender(bbsCodeService.getGenderCode(gender));
        p.setDateOfBirth(new SimpleDateFormat(Constants.ISO_DATE_FORMAT).format(dateOfBirth));
        p.setOccupation(bbsCodeService.getOccupationCode(occupation));
        p.setEducationLevel(bbsCodeService.getEducationCode(educationLevel));
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
        nationalIdAttrType.setName(Constants.NATIONAL_ID_ATTRIBUTE);
        attributes.add(new PersonAttribute(nationalIdAttrType, nationalId));

        final PersonAttributeType healthIdAttrType = new PersonAttributeType();
        healthIdAttrType.setName(Constants.HEALTH_ID_ATTRIBUTE);
        attributes.add(new PersonAttribute(healthIdAttrType, healthId));

        final PersonAttributeType occupationAttrType = new PersonAttributeType();
        occupationAttrType.setName(Constants.OCCUPATION_ATTRIBUTE);
        attributes.add(new PersonAttribute(occupationAttrType, occupation));

        final PersonAttributeType educationAttrType = new PersonAttributeType();
        educationAttrType.setName(Constants.EDUCATION_ATTRIBUTE);
        attributes.add(new PersonAttribute(educationAttrType, educationLevel));

        final PersonAttributeType primaryContactAttrType = new PersonAttributeType();
        primaryContactAttrType.setName(Constants.PRIMARY_CONTACT_ATTRIBUTE);
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
    public void dummy () {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date beginTime = new Date(cal.getTimeInMillis());

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);

        Date endTime = new Date(cal.getTimeInMillis());

        System.out.println("current:" + now);
        System.out.println("begin:" + beginTime);
        System.out.println("end:" + endTime);


    }
}
