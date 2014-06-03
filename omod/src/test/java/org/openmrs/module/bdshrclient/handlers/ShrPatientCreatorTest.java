package org.openmrs.module.bdshrclient.handlers;

import org.ict4h.atomfeed.client.domain.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.api.PatientService;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;
import org.openmrs.module.bdshrclient.util.GenderEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ShrPatientCreatorTest {

    @Mock
    private PatientService patientService;
    @Mock
    private AddressHierarchyService addressHierarchyService;
    private ShrPatientCreator shrPatientCreator;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        shrPatientCreator = new ShrPatientCreator(addressHierarchyService, patientService);
    }


    @Test
    public void shouldCreatePatientFromEvent() throws Exception {
        final String uuid = "123abc456";
        final String givenName = "Sachin";
        final String middleName = "Ramesh";
        final String familyName = "Tendulkar";
        final String gender = "Male";
        final String division = "some-division";
        final String district = "some-district";
        final String upazilla = "some-upazilla";
        final String union = "some-union";

        Person person = new Person();
        PersonName personName = new PersonName(givenName, middleName, familyName);
        person.addName(personName);
        person.setGender(gender);

        PersonAddress address = new PersonAddress();
        address.setStateProvince(division);
        address.setCountyDistrict(district);
        address.setAddress3(upazilla);
        address.setCityVillage(union);
        person.addAddress(address);
        org.openmrs.Patient openMrsPatient = new org.openmrs.Patient(person);

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
        entry1.setUserGeneratedId("10");
        entries1.add(entry1);

        List<AddressHierarchyEntry> entries2 = new ArrayList<AddressHierarchyEntry>();
        AddressHierarchyEntry entry2 = new AddressHierarchyEntry();
        entry2.setUserGeneratedId("1020");
        entries2.add(entry2);

        List<AddressHierarchyEntry> entries3 = new ArrayList<AddressHierarchyEntry>();
        AddressHierarchyEntry entry3 = new AddressHierarchyEntry();
        entry3.setUserGeneratedId("102030");
        entries3.add(entry3);

        List<AddressHierarchyEntry> entries4 = new ArrayList<AddressHierarchyEntry>();
        AddressHierarchyEntry entry4 = new AddressHierarchyEntry();
        entry4.setUserGeneratedId("10203040");
        entries4.add(entry4);


        List<AddressHierarchyLevel> levels = Arrays.asList(level1, level2, level3, level4);

        when(addressHierarchyService.getAddressHierarchyLevels()).thenReturn(levels);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(0), division)).thenReturn(entries1);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(1), district)).thenReturn(entries2);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(2), upazilla)).thenReturn(entries3);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(3), union)).thenReturn(entries4);


        when(patientService.getPatientByUuid(uuid)).thenReturn(openMrsPatient);

        Event event = new Event("id100", "/openmrs/ws/rest/v1/patient/" + uuid + "?v=full");
        Patient patient = shrPatientCreator.populatePatient(event);

        assertEquals("Sachin", patient.getFirstName());
        assertEquals("Ramesh", patient.getMiddleName());
        assertEquals("Tendulkar", patient.getLastName());
        assertEquals(GenderEnum.MALE.getId(), patient.getGender());
        assertEquals(new Address("10", "1020", "102030", "10203040"), patient.getAddress());
    }
}
