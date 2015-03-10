package org.openmrs.module.shrclient.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.openmrs.module.fhir.utils.PropertyKeyConstants.FACILITY_ID;
import static org.openmrs.module.fhir.utils.PropertyKeyConstants.FACILITY_URL_FORMAT;

public class SystemPropertiesTest {
    @Test
    public void shouldReadFacilityId() throws Exception {
        Properties instanceProperties = new Properties();
        instanceProperties.setProperty(FACILITY_ID, "foo");
        SystemProperties systemProperties = new SystemProperties(new HashMap<String, String>(), new Properties(), new Properties(), new Properties(), instanceProperties);
        assertThat(systemProperties.getFacilityId(), is("foo"));
    }


    @Test
    public void shouldReadFRProperties() throws Exception {
        Properties frProperties = new Properties();
        frProperties.setProperty(FACILITY_URL_FORMAT, "bar");
        SystemProperties systemProperties = new SystemProperties(new HashMap<String, String>(),
                frProperties, new Properties(), new Properties(), new Properties());
        assertThat(systemProperties.getFacilityUrlFormat(), is("bar"));
    }

    @Test
    public void shouldReadBaseUrls() throws Exception {
        Properties shrProperties = new Properties();
        HashMap<String, String> baseUrls = new HashMap<>();
        baseUrls.put("mci", "https://boogiewoogie:8080");
        baseUrls.put("fr", "https://furrr:8080");
        SystemProperties systemProperties = new SystemProperties(baseUrls, new Properties(), new Properties(), new Properties(), new Properties());
        assertThat(systemProperties.getMciPatientUrl(), is("https://boogiewoogie:8080/api/v1/patients"));
        assertThat(systemProperties.getFrBaseUrl(), is("https://furrr:8080/"));
    }

    @Test
    public void shouldReadValueSetUrls() throws Exception {
        Properties trProperties = new Properties();
        trProperties.setProperty("tr.base.valueset.url", "openmrs/ws/rest/v1/tr/vs");
        trProperties.setProperty("tr.valueset.route", "sample-route");
        trProperties.setProperty("tr.valueset.quantityunits", "sample-units");
        HashMap<String, String> baseUrls = new HashMap<>();
        baseUrls.put("tr", "http://172.18.46.56:9080");
        SystemProperties systemProperties = new SystemProperties(baseUrls, new Properties(), trProperties, new Properties(), new Properties());
        assertThat(systemProperties.getTrValuesetUrl("route"), is("http://172.18.46.56:9080/openmrs/ws/rest/v1/tr/vs/sample-route"));
        assertThat(systemProperties.getTrValuesetUrl("quantityunits"), is("http://172.18.46.56:9080/openmrs/ws/rest/v1/tr/vs/sample-units"));
    }
}