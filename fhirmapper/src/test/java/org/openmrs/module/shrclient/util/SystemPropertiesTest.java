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
        SystemProperties systemProperties = new SystemProperties(new HashMap<String, String>(), shrProperties, new Properties());
        assertThat(systemProperties.getFacilityId(), is("foo"));
    }


    @Test
    public void shouldReadFRProperties() throws Exception {
        Properties frProperties = new Properties();
        frProperties.setProperty(SystemProperties.FACILITY_URL_FORMAT, "bar");
        SystemProperties systemProperties = new SystemProperties(new HashMap<String, String>(),
                new Properties(), frProperties);
        assertThat(systemProperties.getFacilityUrlFormat(), is("bar"));
    }

    @Test
    public void shouldReadBaseUrls() throws Exception {
        Properties shrProperties = new Properties();
        HashMap<String, String> baseUrls = new HashMap<>();
        baseUrls.put("mci", "https://boogiewoogie:8080");
        baseUrls.put("fr", "https://furrr:8080");
        SystemProperties systemProperties = new SystemProperties(baseUrls, shrProperties, new Properties());
        assertThat(systemProperties.getMciPatientUrl(), is("https://boogiewoogie:8080/patients/"));
        assertThat(systemProperties.getFrBaseUrl(), is("https://furrr:8080/"));
    }
}