package org.openmrs.module.shrclient.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SystemPropertiesTest {
    @Test
    public void shouldReadFacilityId() throws Exception {
        Properties shrProperties = new Properties();
        shrProperties.setProperty(SystemProperties.FACILITY_ID, "foo");
        SystemProperties systemProperties = new SystemProperties(new HashMap<String, String>(), shrProperties);
        assertThat(systemProperties.getFacilityId(), is("foo"));
    }

    @Test
    public void shouldReadBaseUrls() throws Exception {
        Properties shrProperties = new Properties();
        HashMap<String, String> baseUrls = new HashMap<>();
        baseUrls.put("mci", "https://boogiewoogie:8080");
        SystemProperties systemProperties = new SystemProperties(baseUrls, shrProperties);
        assertThat(systemProperties.getMciPatientUrl(), is("https://boogiewoogie:8080/patients/"));

    }
}