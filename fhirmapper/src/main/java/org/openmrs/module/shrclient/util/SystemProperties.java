package org.openmrs.module.shrclient.util;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.fhir.utils.PropertyKeyConstants;

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
                            Properties mciProperties) {
        this.baseUrls = baseUrls;
        this.frProperties = frProperties;
        this.trProperties = trProperties;
        this.prProperties = prProperties;
        this.facilityInstanceProperties = facilityInstanceProperties;
        this.mciProperties = mciProperties;
    }

    public String getFacilityId() {
        return facilityInstanceProperties.getProperty(FACILITY_ID);
    }

    public String getTrValuesetUrl(String valueSetKeyName) {
        String valuesetName = trProperties.getProperty(valueSetKeyName);
        if (StringUtils.isBlank(valuesetName)) {
            throw new RuntimeException("Could not identify valueset. Make sure tr properties has key:" + valueSetKeyName);
        }

        String trBaseUrl = StringUtil.ensureSuffix(trProperties.getProperty(PropertyKeyConstants.TR_REFERENCE_PATH), "/");
        String trValueSetPathInfo = StringUtil.removePrefix(trProperties.getProperty(TR_VALUESET_PATH_INFO), "/");

        return StringUtil.ensureSuffix(trBaseUrl + trValueSetPathInfo, "/") + valuesetName;
    }

    public String getProviderResourcePath() {
        return prProperties.getProperty(PROVIDER_REFERENCE_PATH).trim();
    }

    public String getMciPatientUrl() {
        String mciRefPath = mciProperties.getProperty(MCI_REFERENCE_PATH);
        String mciPatientCtx = mciProperties.getProperty(MCI_PATIENT_CONTEXT);
        return StringUtil.ensureSuffix(mciRefPath, "/") + StringUtil.removePrefix(mciPatientCtx, "/");
    }

    public String getFacilityResourcePath() {
        return frProperties.getProperty(FACILITY_REFERENCE_PATH).trim();
    }

}
