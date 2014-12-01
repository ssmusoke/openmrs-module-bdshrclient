package org.openmrs.module.shrclient.util;

import java.util.Map;
import java.util.Properties;

public class SystemProperties {
    public static final String FACILITY_ID = "shr.facilityId";
    public static final String FACILITY_URL_FORMAT = "fr.facilityUrlFormat";
    private Map<String, String> baseUrls;
    private Properties shrProperties;
    private Properties frProperties;

    public SystemProperties(Map<String, String> baseUrls, Properties shrProperties, Properties frProperties) {
        this.baseUrls = baseUrls;
        this.shrProperties = shrProperties;
        this.frProperties = frProperties;
    }

    public String getFacilityId() {
        return shrProperties.getProperty(FACILITY_ID);
    }

    public String getMciPatientUrl() {
        return baseUrls.get("mci") + "/patients/";
    }

    public String getFrBaseUrl() {
        return baseUrls.get("fr") + "/";
    }

    public String getFacilityUrlFormat() {
        return frProperties.getProperty(FACILITY_URL_FORMAT);
    }
}
