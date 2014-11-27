package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.AtomFeed;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class CompositionBundleCreatorIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    CompositionBundleCreator bundleCreator;

    @Test
    public void shouldWireAllResourceHandlers() throws Exception {
        ensureBundleCreatorHasResourceHandlers("obsResourceHandlers");
        ensureBundleCreatorHasResourceHandlers("orderResourceHandlers");
    }

    private void ensureBundleCreatorHasResourceHandlers(String handlerName) throws NoSuchFieldException, IllegalAccessException {
        final Field field = bundleCreator.getClass().getDeclaredField(handlerName);
        field.setAccessible(true);
        Object instances = field.get(bundleCreator);
        assertNotNull(instances);
        if (instances instanceof List) {
            assertTrue(((List) instances).size() > 0);
        }
    }

    @Test
    public void shouldCreateFhirBundle() throws Exception {
        executeDataSet("shrClientBundleCreatorTestDS.xml");
        String facilityId = "10000036";
        AtomFeed bundle = bundleCreator.compose(Context.getEncounterService().getEncounter(36), facilityId);
        assertNotNull(bundle);
    }
}
