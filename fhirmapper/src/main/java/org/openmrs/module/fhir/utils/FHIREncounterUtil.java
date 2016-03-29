package org.openmrs.module.fhir.utils;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import org.openmrs.Location;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.shrclient.util.StringUtil;
import org.openmrs.module.shrclient.util.SystemProperties;

public class FHIREncounterUtil {
    public static String getSHREncounterUrl(String shrEncounterId, String healthId, SystemProperties systemProperties) {
        String shrEncounterRefUrl = systemProperties.getShrEncounterUrl();
        return StringUtil.ensureSuffix(String.format(shrEncounterRefUrl, healthId), "/") + shrEncounterId;
    }

    public static String getFacilityId(Bundle bundle) {
        Encounter shrEncounter = FHIRBundleHelper.getEncounter(bundle);
        return new EntityReference().parse(Location.class, shrEncounter.getServiceProvider().getReference().getValue());
    }
}
