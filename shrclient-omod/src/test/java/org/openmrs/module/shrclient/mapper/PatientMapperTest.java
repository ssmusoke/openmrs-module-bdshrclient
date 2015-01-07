package org.openmrs.module.shrclient.mapper;

import org.openmrs.module.addresshierarchy.AddressField;
import org.openmrs.module.shrclient.mci.api.model.Address;
import org.openmrs.module.shrclient.mci.api.model.Patient;
import org.openmrs.module.shrclient.service.BbsCodeService;
import org.openmrs.module.shrclient.service.impl.BbsCodeServiceImpl;
import org.openmrs.module.fhir.utils.Constants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.*;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.shrclient.util.AddressHelper;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PatientMapperTest {

    @Mock
    private AddressHierarchyService addressHierarchyService;

    private AddressHelper addressHelper;
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
        addressHelper = new AddressHelper(addressHierarchyService);
        patientMapper = new PatientMapper(bbsCodeService, addressHelper);
    }

    @Test
    public void shouldOpenMrsPatientToMciPatient() throws Exception {
        final String givenName = "Sachin";
        final String middleName = "Ramesh";
        final String familyName = "Tendulkar";
        final String gender = "M";
        final Date dateOfBirth = new SimpleDateFormat(Constants.ISO_DATE_FORMAT).parse("2000-12-31");
        final Date dateOfDeath = new SimpleDateFormat(Constants.ISO_DATE_FORMAT).parse("2010-12-31");

        final String addressLine = "house10";
        final String divisionId = "10";
        final String division = "some-division";
        final String districtId = "1020";
        final String district = "some-district";
        final String upazillaId = "102030";
        final String upazilla = "some-upazilla";
        final String cityCorpId = "10203040";
        final String cityCorp = "some-cityCorp";
        final String unionOrUrbanWardId = "1020304050";
        final String unionOrUrbanWard = "some-urban-ward";
        final String ruralWardId = "102030405001";
        final String ruralWard = "some-rural-ward";

        Person person = new Person();
        PersonName personName = new PersonName(givenName, middleName, familyName);
        person.addName(personName);
        person.setGender(gender);
        person.setBirthdate(dateOfBirth);
        person.setDeathDate(dateOfDeath);

        PersonAddress address = new PersonAddress();
        address.setAddress1(addressLine);
        address.setStateProvince(division);
        address.setCountyDistrict(district);
        address.setAddress5(upazilla);
        address.setAddress4(cityCorp);
        address.setAddress3(unionOrUrbanWard);
        address.setAddress2(ruralWard);
        person.addAddress(address);
        org.openmrs.Patient openMrsPatient = new org.openmrs.Patient(person);

        openMrsPatient.setAttributes(createOpenMrsPersonAttributes());

        List<AddressHierarchyEntry> divisionEntries = createAddressHierarchyEntries(divisionId);
        List<AddressHierarchyEntry> districtEntries = createAddressHierarchyEntries(districtId);
        List<AddressHierarchyEntry> upazillaEntries = createAddressHierarchyEntries(upazillaId);
        List<AddressHierarchyEntry> cityCorpEntries = createAddressHierarchyEntries(cityCorpId);
        List<AddressHierarchyEntry> unionOrUrbanWardEntries = createAddressHierarchyEntries(unionOrUrbanWardId);
        List<AddressHierarchyEntry> ruralWardEntries = createAddressHierarchyEntries(ruralWardId);

        when(addressHierarchyService.getAddressHierarchyLevelByAddressField(any(AddressField.class))).thenReturn(new AddressHierarchyLevel());
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(any(AddressHierarchyLevel.class), eq(division))).thenReturn(divisionEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(any(AddressHierarchyLevel.class), eq(district))).thenReturn(districtEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(any(AddressHierarchyLevel.class), eq(upazilla))).thenReturn(upazillaEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(any(AddressHierarchyLevel.class), eq(cityCorp))).thenReturn(cityCorpEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(any(AddressHierarchyLevel.class), eq(unionOrUrbanWard))).thenReturn(unionOrUrbanWardEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(any(AddressHierarchyLevel.class), eq(ruralWard))).thenReturn(ruralWardEntries);

        Patient patient = patientMapper.map(openMrsPatient);

        Patient p = new Patient();
        p.setNationalId(nationalId);
        p.setHealthId(healthId);
        p.setGivenName(givenName);
        p.setSurName(familyName);
        p.setGender(gender);
        p.setDateOfBirth(new SimpleDateFormat(Constants.ISO_DATE_FORMAT).format(dateOfBirth));
        p.setDateOfDeath(new SimpleDateFormat(Constants.ISO_DATE_FORMAT).format(dateOfDeath));
        p.setOccupation(bbsCodeService.getOccupationCode(occupation));
        p.setEducationLevel(bbsCodeService.getEducationCode(educationLevel));
        p.setPrimaryContact(primaryContact);

        Address a = new Address();
        a.setAddressLine(addressLine);
        a.setDivisionId("10");
        a.setDistrictId("20");
        a.setUpazilaId("30");
        a.setCityCorporationId("40");
        a.setUnionOrUrbanWardId("50");
        a.setRuralWardId("01");
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
