package org.openmrs.module.shrclient.model.mci.api.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.shrclient.model.Address;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.model.Relation;

import static org.junit.Assert.assertEquals;

public class PatientJsonTest {

    private Patient patient;

    @Before
    public void setup() {
        patient = new Patient();
        patient.setGivenName("Scott");
        patient.setSurName("Tiger");
        patient.setGender("M");
        Address address = new Address();
        address.setDivisionId("10");
        address.setDistrictId("04");
        address.setUpazilaId("09");
        address.setCityCorporationId("20");
        address.setUnionOrUrbanWardId("01");
        patient.setAddress(address);
    }

    @Test
    public void shouldExcludeEmptyNonMandatoryFields() throws Exception {
        String expected = "{\"given_name\":\"Scott\",\"sur_name\":\"Tiger\",\"date_of_birth\":null,\"gender\":\"M\"" +
                ",\"present_address\":{\"address_line\":null,\"division_id\":\"10\",\"district_id\":\"04\"" +
                ",\"upazila_id\":\"09\",\"city_corporation_id\":\"20\",\"union_or_urban_ward_id\":\"01\"},\"status\":null,\"date_of_death\":null}";
        String actual = new ObjectMapper().writeValueAsString(patient);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldIncludeNonEmptyNonMandatoryFields() throws Exception {
        patient.setNationalId("nid-100");
        String expected = "{\"nid\":\"nid-100\",\"given_name\":\"Scott\",\"sur_name\":\"Tiger\",\"date_of_birth\":null," +
                "\"gender\":\"M\",\"present_address\":{\"address_line\":null,\"division_id\":\"10\",\"district_id\":\"04\"," +
                "\"upazila_id\":\"09\",\"city_corporation_id\":\"20\",\"union_or_urban_ward_id\":\"01\"},\"status\":null,\"date_of_death\":null}";
        String actual = new ObjectMapper().writeValueAsString(patient);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldIncludeRelationsForPatient() throws Exception {
        patient.setNationalId("nid-100");
        patient.setRelations(getRelationsForPatient(patient));
        String expected = "{\"nid\":\"nid-100\",\"given_name\":\"Scott\",\"sur_name\":\"Tiger\",\"date_of_birth\":null," +
                "\"gender\":\"M\",\"present_address\":{\"address_line\":null,\"division_id\":\"10\",\"district_id\":\"04\"," +
                "\"upazila_id\":\"09\",\"city_corporation_id\":\"20\",\"union_or_urban_ward_id\":\"01\"}," +
                "\"relations\":[{"+"\"type\":\"mother\"," +
                "\"given_name\":\"Mother of Scott\"," +
                "\"sur_name\":\"Tiger\"}],\"status\":null,\"date_of_death\":null}";
        String actual = new ObjectMapper().writeValueAsString(patient);
        System.out.println(actual);
        assertEquals(expected, actual);
    }

    private Relation[] getRelationsForPatient(Patient patient) {
        Relation aRel = new Relation();
        aRel.setGivenName("Mother of " + patient.getGivenName());
        aRel.setSurName(patient.getSurName());
        aRel.setType("mother");
        Relation[] relations = new Relation[1];
        relations[0] = aRel;
        return relations;
    }

}