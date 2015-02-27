package org.openmrs.module.shrclient.util;

import org.openmrs.module.fhir.utils.Constants;

import java.util.Map;
import java.util.Properties;

public class SystemProperties {
    public static final String FACILITY_ID = "facility.facilityId";
    public static final String FACILITY_URL_FORMAT = "fr.facilityUrlFormat";
    public static final String TR_VALUESET_URL = "tr.base.valueset.url";
    public static final String PROVIDER_URL_FORMAT = "pr.providerUrlFormat";
    private Map<String, String> baseUrls;
    private Properties frProperties;
    private Properties trProperties;
    private Properties prProperties;
    private Properties facilityInstanceProperties;

    public SystemProperties(Map<String, String> baseUrls, Properties frProperties,
                            Properties trProperties, Properties prProperties, Properties facilityInstanceProperties) {
        this.baseUrls = baseUrls;
        this.frProperties = frProperties;
        this.trProperties = trProperties;
        this.prProperties = prProperties;
        this.facilityInstanceProperties = facilityInstanceProperties;
    }

    public String getFacilityId() {
        return facilityInstanceProperties.getProperty(FACILITY_ID);
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

    public String getProviderUrlFormat() {
        return baseUrls.get("pr") + prProperties.getProperty(PROVIDER_URL_FORMAT);
    }
}
