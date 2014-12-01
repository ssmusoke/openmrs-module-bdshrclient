package org.openmrs.module.fhir.mapper.model;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class EntityReference {

    static Map<Type, EntityReference> referenceMap;
    static {
        referenceMap = new HashMap<>();
        referenceMap.put(Patient.class, new PatientReference());
        referenceMap.put(Encounter.class, new EncounterReference());
    }

    public String build(Type type, SystemProperties systemProperties, String id) {
        return referenceMap.get(type).create(id, systemProperties);
    }

    protected String create(String id, SystemProperties systemProperties) {
        return id;
    }


    private static class PatientReference extends EntityReference {

        @Override
        public String create(String id, SystemProperties systemProperties) {
            return systemProperties.getMciPatientUrl() + id;
        }
    }

    private static class EncounterReference extends EntityReference {
        @Override
        protected String create(String id, SystemProperties systemProperties) {
            return "urn:" +  id;
        }
    }
}
