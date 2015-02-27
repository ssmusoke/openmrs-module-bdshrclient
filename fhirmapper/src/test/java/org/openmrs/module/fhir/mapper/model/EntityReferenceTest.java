package org.openmrs.module.fhir.mapper.model;

import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class EntityReferenceTest {
    @Test
    public void shouldDefaultToIdForTypesNotDefined() {
        EntityReference entityReference = new EntityReference();
        assertEquals("urn:1", entityReference.build(Integer.class, getSystemProperties("1234"), "1"));
    }

    @Test
    public void shouldCreatePatientReference() {
        EntityReference entityReference = new EntityReference();
        assertEquals("http://mci/api/v1/patients/1", entityReference.build(Patient.class, getSystemProperties("1234"), "1"));
    }

    @Test
    public void shouldCreateEncounterReference() {
        EntityReference entityReference = new EntityReference();
        assertEquals("urn:1", entityReference.build(Encounter.class, getSystemProperties("1234"), "1"));
    }

    @Test
    public void shouldCreateFacilityLocationReference() {
        EntityReference entityReference = new EntityReference();
        assertEquals("http://fr/foo-bar/1234.json", entityReference.build(Location.class, getSystemProperties("1234"), "1234"));
    }

    @Test
    public void shouldParseFacilityLocationReference() throws Exception {
        EntityReference entityReference = new EntityReference();
        String facilityId = "1013101";
        assertEquals(facilityId, entityReference.parse(Location.class, "http://fr.com/hrm/" + facilityId + ".json"));
    }

    @Test
    public void shouldParseMciPatientUrl() throws Exception {
        EntityReference entityReference = new EntityReference();
        String hid = "hid";
        assertEquals(hid, entityReference.parse(Patient.class, "http://mci.com/api/v1/patient/" + hid));
    }

    private SystemProperties getSystemProperties(String facilityId) {
        Properties shrProperties = new Properties();
        shrProperties.setProperty(SystemProperties.FACILITY_ID, facilityId);
        Properties trProperties = new Properties();

        Properties frProperties = new Properties();
        frProperties.setProperty(SystemProperties.FACILITY_URL_FORMAT, "foo-bar/%s.json");

        Properties prPoperties = new Properties();
        Properties facilityInstanceProperties = new Properties();

        HashMap<String, String> baseUrls = new HashMap<>();
        baseUrls.put("mci", "http://mci");
        baseUrls.put("fr", "http://fr");

        return new SystemProperties(baseUrls, frProperties, trProperties, prPoperties, facilityInstanceProperties);
    }
}