package org.openmrs.module.shrclient.util;

import org.openmrs.module.fhir.utils.Constants;

import java.util.Map;
import java.util.Properties;

public class SystemProperties {
    public static final String FACILITY_ID = "shr.facilityId";
    public static final String FACILITY_URL_FORMAT = "fr.facilityUrlFormat";
    public static final String TR_VALUESET_URL = "tr.base.valueset.url";
    private Map<String, String> baseUrls;
    private Properties shrProperties;
    private Properties frProperties;
    private Properties trProperties;

    public SystemProperties(Map<String, String> baseUrls, Properties shrProperties, Properties frProperties, Properties trProperties) {
        this.baseUrls = baseUrls;
        this.shrProperties = shrProperties;
        this.frProperties = frProperties;
        this.trProperties = trProperties;
    }

    public String getFacilityId() {
        return shrProperties.getProperty(FACILITY_ID);
    }

    public String getMciPatientUrl() {
        String mciBaseUrl = baseUrls.get("mci");
        if (mciBaseUrl.endsWith("/")) {
            return mciBaseUrl +  Constants.MCI_PATIENT_URL.substring(1);
        } else {
            return mciBaseUrl + Constants.MCI_PATIENT_URL;
        }
    }

    public String getFrBaseUrl() {
        return baseUrls.get("fr") + "/";
    }

    public String getFacilityUrlFormat() {
        return frProperties.getProperty(FACILITY_URL_FORMAT);
    }

    public String getTrValuesetUrl(String valueSetName) {
        return baseUrls.get("tr")+ "/" + trProperties.getProperty(TR_VALUESET_URL) + "/" + trProperties.getProperty("tr.valueset." + valueSetName);
    }
}
