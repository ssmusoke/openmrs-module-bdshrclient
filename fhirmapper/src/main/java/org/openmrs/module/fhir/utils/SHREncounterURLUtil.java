package org.openmrs.module.fhir.utils;

import org.openmrs.module.shrclient.util.StringUtil;
import org.openmrs.module.shrclient.util.SystemProperties;

public class SHREncounterURLUtil {
    public static String getEncounterUrl(String shrEncounterId, String healthId, SystemProperties systemProperties) {
        String shrEncounterRefUrl = systemProperties.getShrEncounterUrl();
        return StringUtil.ensureSuffix(String.format(shrEncounterRefUrl, healthId), "/") + shrEncounterId;
    }
}
