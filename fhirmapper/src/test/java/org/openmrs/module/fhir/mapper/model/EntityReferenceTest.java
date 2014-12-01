package org.openmrs.module.fhir.mapper.model;

import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class EntityReferenceTest {
    @Test
    public void shouldCreatePatientReference() throws Exception {
        EntityReference entityReference = new EntityReference();
        assertEquals("http://mci/patients/1", entityReference.build(Patient.class, getSystemProperties("1234"), "1"));
    }

    @Test
    public void shouldCreateEncounterReference() throws Exception {
        EntityReference entityReference = new EntityReference();
        assertEquals("urn:1", entityReference.build(Encounter.class, getSystemProperties("1234"), "1"));
    }

    private SystemProperties getSystemProperties(String facilityId) {
        Properties shrProperties = new Properties();
        shrProperties.setProperty(SystemProperties.FACILITY_ID, facilityId);
        HashMap<String, String> baseUrls = new HashMap<>();
        baseUrls.put("mci", "http://mci");
        return new SystemProperties(baseUrls, shrProperties);
    }
}