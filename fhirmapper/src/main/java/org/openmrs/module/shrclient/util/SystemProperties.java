package org.openmrs.module.shrclient.util;

import java.util.Map;
import java.util.Properties;

public class SystemProperties {
    public static final String FACILITY_ID = "shr.facilityId";
    private String facilityId;
    private Map<String, String> baseUrls;

    public SystemProperties(Map<String, String> baseUrls, Properties shrProperties) {
        this.baseUrls = baseUrls;
        facilityId = shrProperties.getProperty(FACILITY_ID);
    }

    public String getFacilityId() {
        return facilityId;
    }

    public String getMciPatientUrl() {
        return baseUrls.get("mci") + "/patients/";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SystemProperties that = (SystemProperties) o;

        if (baseUrls != null ? !baseUrls.equals(that.baseUrls) : that.baseUrls != null) return false;
        if (facilityId != null ? !facilityId.equals(that.facilityId) : that.facilityId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = facilityId != null ? facilityId.hashCode() : 0;
        result = 31 * result + (baseUrls != null ? baseUrls.hashCode() : 0);
        return result;
    }
}
