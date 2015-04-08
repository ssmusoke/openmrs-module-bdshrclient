package org.openmrs.module.shrclient.util;

import org.openmrs.module.fhir.utils.Constants;

import java.util.Map;
import java.util.Properties;

import static org.openmrs.module.fhir.utils.PropertyKeyConstants.*;

public class SystemProperties {
    private Map<String, String> baseUrls;
    private Properties frProperties;
    private Properties trProperties;
    private Properties prProperties;
    private Properties facilityInstanceProperties;
    private Properties mciProperties;
    private Properties shrProperties;

    public SystemProperties(Map<String, String> baseUrls, Properties frProperties,
                            Properties trProperties, Properties prProperties,
                            Properties facilityInstanceProperties,
                            Properties mciProperties, Properties shrProperties) {
        this.baseUrls = baseUrls;
        this.frProperties = frProperties;
        this.trProperties = trProperties;
        this.prProperties = prProperties;
        this.facilityInstanceProperties = facilityInstanceProperties;
        this.mciProperties = mciProperties;
        this.shrProperties = shrProperties;
    }

    public String getFacilityId() {
        return facilityInstanceProperties.getProperty(FACILITY_ID);
    }

    public String getMciPatientUrl() {
        String mciBaseUrl = baseUrls.get("mci");
        if (mciBaseUrl.endsWith("/")) {
            return mciBaseUrl + Constants.MCI_PATIENT_URL.substring(1);
        } else {
            return mciBaseUrl + Constants.MCI_PATIENT_URL;
        }
    }

    public String getFrBaseUrl() {
        return baseUrls.get("fr") + "/";
    }

    public String getTrValuesetUrl(String valueSetName) {
        return baseUrls.get("tr") + "/" + trProperties.getProperty(TR_VALUESET_URL) + "/" + trProperties.getProperty(TR_VALUESET_KEY + valueSetName);
    }

    public String getProviderResourcePath() {
        return prProperties.getProperty(PROVIDER_REFERENCE_PATH);
    }

    public String getMciPatientPublicUrl() {
        String mciBaseUrl = mciProperties.getProperty(MCI_PUBLIC_URL_BASE);
        if (mciBaseUrl.endsWith("/")) {
            return mciBaseUrl + Constants.MCI_PATIENT_URL.substring(1);
        } else {
            return mciBaseUrl + Constants.MCI_PATIENT_URL;
        }
    }

    public String getFacilityResourcePath() {
        return frProperties.getProperty(FACILITY_REFERENCE_PATH).trim();
    }

    public Properties getShrProperties() {
        return shrProperties;
    }
}
