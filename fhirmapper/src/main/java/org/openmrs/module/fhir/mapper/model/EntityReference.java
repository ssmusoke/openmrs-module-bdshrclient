package org.openmrs.module.fhir.mapper.model;

import org.apache.commons.collections4.map.DefaultedMap;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.module.shrclient.util.StringUtil;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.lang.reflect.Type;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;

public class EntityReference {

    static DefaultedMap <Type, EntityReference> referenceMap;
    static {
        referenceMap = new DefaultedMap<>(new EntityReference());
        referenceMap.put(Patient.class, new PatientReference());
        referenceMap.put(Encounter.class, new EncounterReference());
        referenceMap.put(Location.class, new FacilityReference());
        referenceMap.put(EncounterProvider.class, new ProviderReference());
    }

    public String build(Type type, SystemProperties systemProperties, String id) {
        return referenceMap.get(type).create(id, systemProperties);
    }

    public String parse(Type type, String url) {
        return referenceMap.get(type).parseUrl(url);
    }

    protected String parseUrl(String url) {
        return url;
    }

    protected String create(String id, SystemProperties systemProperties) {
        return "urn:" + id;
    }

    private static class PatientReference extends EntityReference {

        @Override
        public String create(String id, SystemProperties systemProperties) {
            return StringUtil.ensureSuffix(systemProperties.getMciPatientUrl(), "/") + id;
        }

        @Override
        protected String parseUrl(String url) {
            return substringAfterLast(url, "/");
        }
    }

    private static class EncounterReference extends EntityReference {
        @Override
        protected String create(String id, SystemProperties systemProperties) {
            return "urn:" +  id;
        }
    }

    private static class FacilityReference extends EntityReference {
        @Override
        protected String create(String id, SystemProperties systemProperties) {
            return String.format("%s/%s.json",
                    StringUtil.removeSuffix(systemProperties.getFacilityResourcePath(), "/"), id);
        }

        @Override
        protected String parseUrl(String facilityUrl) {
            String s = StringUtils.substringAfterLast(facilityUrl, "/");
            return StringUtils.substringBefore(s, ".json");
        }
    }

    private static class ProviderReference extends EntityReference {
        @Override
        protected String create(String identifier, SystemProperties systemProperties) {
            return String.format("%s/%s.json",
                    StringUtil.removeSuffix(systemProperties.getProviderResourcePath(), "/"), identifier);
        }

        @Override
        protected String parseUrl(String providerUrl) {
            String s = StringUtils.substringAfterLast(providerUrl, "/");
            return StringUtils.substringBefore(s, ".json");
        }
    }
}
