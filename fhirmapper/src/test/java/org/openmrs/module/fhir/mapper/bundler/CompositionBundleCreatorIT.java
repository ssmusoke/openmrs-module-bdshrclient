package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.AtomFeed;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class CompositionBundleCreatorIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    CompositionBundle compositionBundle;

    @Test
    public void shouldWireAllResourceHandlers() throws Exception {
        ensureBundleCreatorHasResourceHandlers("obsResourceHandlers");
        ensureBundleCreatorHasResourceHandlers("orderResourceHandlers");
    }

    private void ensureBundleCreatorHasResourceHandlers(String handlerName) throws NoSuchFieldException, IllegalAccessException {
        final Field field = compositionBundle.getClass().getDeclaredField(handlerName);
        field.setAccessible(true);
        Object instances = field.get(compositionBundle);
        assertNotNull(instances);
        if (instances instanceof List) {
            assertTrue(((List) instances).size() > 0);
        }
    }

    @Test
    public void shouldCreateFhirBundle() throws Exception {
        executeDataSet("shrClientBundleCreatorTestDS.xml");
        String facilityId = "10000036";
        AtomFeed bundle = compositionBundle.create(Context.getEncounterService().getEncounter(36), getSystemProperties(facilityId));
        assertNotNull(bundle);
    }

    private SystemProperties getSystemProperties(String facilityId) {
        Properties shrProperties = new Properties();
        shrProperties.setProperty(SystemProperties.FACILITY_ID, facilityId);
        return new SystemProperties(new HashMap<String, String>(), shrProperties);
    }
}
