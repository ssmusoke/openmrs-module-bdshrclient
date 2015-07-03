package org.openmrs.module.shrclient.mapper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.addresshierarchy.AddressField;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.shrclient.model.Address;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.model.Status;
import org.openmrs.module.shrclient.service.BbsCodeService;
import org.openmrs.module.shrclient.service.impl.BbsCodeServiceImpl;
import org.openmrs.module.shrclient.util.AddressHelper;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.fhir.utils.Constants.*;

public class PatientMapperTest {

    @Mock
    private AddressHierarchyService addressHierarchyService;
    @Mock
    private SystemProperties systemProperties;
    @Mock
    private ProviderService providerService;

    private AddressHelper addressHelper;
    private BbsCodeService bbsCodeService;
    private PatientMapper patientMapper;

    private String nationalId = "nid-100";
    private String healthId = "hid-200";
    private String brnId = "brn-200";
    private String uniqueId = "uid-200";
    private String occupation = "agriculture";
    private String educationLevel = "6th to 9th";
    private String primaryContact = "some contact";
    private String houseHoldCode = "house4";
    
    private org.openmrs.Patient openMrsPatient;
    private Patient patient;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        this.bbsCodeService = new BbsCodeServiceImpl();
        addressHelper = new AddressHelper(addressHierarchyService);
        patientMapper = new PatientMapper(bbsCodeService, addressHelper);
        Context context = new Context();
        ServiceContext serviceContext = ServiceContext.getInstance();
        serviceContext.setService(ProviderService.class, providerService);
        context.setServiceContext(serviceContext);
        setupData();
    }

    @Test
    public void shouldMapOpenMrsPatientToMciPatient() throws Exception {
        Patient expectedPatient = patientMapper.map(openMrsPatient, systemProperties);
        assertEquals(this.patient, expectedPatient);
    }

    @Test
    public void shouldGetProviderFromChangedBy() throws Exception {
        Person changedByPerson = new Person(12);
        openMrsPatient.setChangedBy(new User(changedByPerson));
        Provider provider = new Provider(104);
        provider.setIdentifier("1234");
        provider.setPerson(changedByPerson);
        when(providerService.getProvidersByPerson(changedByPerson)).thenReturn(asList(provider));
        
        Patient expectedPatient = patientMapper.map(openMrsPatient, systemProperties);
        assertEquals("http://pr.com/1234.json", expectedPatient.getProviderReference());
    }

    private void setupData() throws ParseException {
        final String givenName = "Sachin";
        final String middleName = "Ramesh";
        final String familyName = "Tendulkar";
        final String gender = "M";
        final Date dateOfBirth = DateUtil.parseDate("2000-12-31", DateUtil.SIMPLE_DATE_FORMAT);
        final Date dateOfDeath = DateUtil.parseDate("2010-12-31", DateUtil.SIMPLE_DATE_FORMAT);

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
        openMrsPatient = new org.openmrs.Patient(person);

        Person userPerson = new Person(101);
        Provider provider = new Provider(101);
        User user = new User(userPerson);
        String providerIdentifier = "4321";
        provider.setIdentifier(providerIdentifier);
        provider.setPerson(userPerson);
        when(providerService.getProvidersByPerson(userPerson)).thenReturn(asList(provider));
        openMrsPatient.setCreator(user);
        when(systemProperties.getProviderResourcePath()).thenReturn("http://pr.com/");

        openMrsPatient.setAttributes(createOpenMrsPersonAttributes());

        List<AddressHierarchyEntry> divisionEntries = createAddressHierarchyEntries(divisionId);
        List<AddressHierarchyEntry> districtEntries = createAddressHierarchyEntries(districtId);
        List<AddressHierarchyEntry> upazillaEntries = createAddressHierarchyEntries(upazillaId);
        List<AddressHierarchyEntry> cityCorpEntries = createAddressHierarchyEntries(cityCorpId);
        List<AddressHierarchyEntry> unionOrUrbanWardEntries = createAddressHierarchyEntries(unionOrUrbanWardId);
        List<AddressHierarchyEntry> ruralWardEntries = createAddressHierarchyEntries(ruralWardId);

        when(addressHierarchyService.getAddressHierarchyLevelByAddressField(any(AddressField.class))).thenReturn(new AddressHierarchyLevel());
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(any(AddressHierarchyLevel.class), eq(division))).thenReturn(divisionEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndNameAndParent(any(AddressHierarchyLevel.class), eq(district), any(AddressHierarchyEntry.class))).thenReturn(districtEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndNameAndParent(any(AddressHierarchyLevel.class), eq(upazilla), any(AddressHierarchyEntry.class))).thenReturn(upazillaEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndNameAndParent(any(AddressHierarchyLevel.class), eq(cityCorp), any(AddressHierarchyEntry.class))).thenReturn(cityCorpEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndNameAndParent(any(AddressHierarchyLevel.class), eq(unionOrUrbanWard), any(AddressHierarchyEntry.class))).thenReturn(unionOrUrbanWardEntries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndNameAndParent(any(AddressHierarchyLevel.class), eq(ruralWard), any(AddressHierarchyEntry.class))).thenReturn(ruralWardEntries);



        patient = new Patient();
        patient.setNationalId(nationalId);
        patient.setHealthId(healthId);
        patient.setBirthRegNumber(brnId);
        patient.setHouseHoldCode(houseHoldCode);
        patient.setGivenName(givenName);
        patient.setSurName(familyName);
        patient.setGender(gender);
        patient.setDateOfBirth(DateUtil.toDateString(dateOfBirth, DateUtil.ISO_DATE_IN_HOUR_MIN_FORMAT));
        patient.setOccupation(bbsCodeService.getOccupationCode(occupation));
        patient.setEducationLevel(bbsCodeService.getEducationCode(educationLevel));
        patient.setProviderReference("http://pr.com/" + providerIdentifier + ".json");

        Status status = new Status();
        status.setType('2');
        status.setDateOfDeath(DateUtil.toDateString(dateOfDeath, DateUtil.ISO_DATE_IN_HOUR_MIN_FORMAT));
        patient.setStatus(status);


        Address a = new Address();
        a.setAddressLine(addressLine);
        a.setDivisionId("10");
        a.setDistrictId("20");
        a.setUpazilaId("30");
        a.setCityCorporationId("40");
        a.setUnionOrUrbanWardId("50");
        a.setRuralWardId("01");
        patient.setAddress(a);
    }

    private Set<PersonAttribute> createOpenMrsPersonAttributes() {
        Set<PersonAttribute> attributes = new HashSet<>();
        final PersonAttributeType nationalIdAttrType = new PersonAttributeType();
        nationalIdAttrType.setName(NATIONAL_ID_ATTRIBUTE);
        attributes.add(new PersonAttribute(nationalIdAttrType, nationalId));

        final PersonAttributeType healthIdAttrType = new PersonAttributeType();
        healthIdAttrType.setName(HEALTH_ID_ATTRIBUTE);
        attributes.add(new PersonAttribute(healthIdAttrType, healthId));

        final PersonAttributeType brnIdAttrType = new PersonAttributeType();
        brnIdAttrType.setName(BIRTH_REG_NO_ATTRIBUTE);
        attributes.add(new PersonAttribute(brnIdAttrType, brnId));

        final PersonAttributeType occupationAttrType = new PersonAttributeType();
        occupationAttrType.setName(OCCUPATION_ATTRIBUTE);
        attributes.add(new PersonAttribute(occupationAttrType, occupation));

        final PersonAttributeType educationAttrType = new PersonAttributeType();
        educationAttrType.setName(EDUCATION_ATTRIBUTE);
        attributes.add(new PersonAttribute(educationAttrType, educationLevel));

        final PersonAttributeType houseHoldAttrType = new PersonAttributeType();
        houseHoldAttrType.setName(HOUSE_HOLD_CODE_ATTRIBUTE);
        attributes.add(new PersonAttribute(houseHoldAttrType, houseHoldCode));
        return attributes;
    }

    private List<AddressHierarchyEntry> createAddressHierarchyEntries(String id) {
        List<AddressHierarchyEntry> entries1 = new ArrayList<>();
        AddressHierarchyEntry entry1 = new AddressHierarchyEntry();
        entry1.setUserGeneratedId(id);
        entries1.add(entry1);
        return entries1;
    }
}
