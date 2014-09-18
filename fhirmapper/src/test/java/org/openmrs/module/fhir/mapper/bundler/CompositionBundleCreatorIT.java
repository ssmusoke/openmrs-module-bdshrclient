package org.openmrs.module.fhir.mapper.bundler;

import static junit.framework.Assert.assertEquals;
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
<<<<<<< HEAD
        Field obsField = bundleCreator.getClass().getDeclaredField("obsResourceHandlers");
        obsField.setAccessible(true);
        Object obsInstances = obsField.get(bundleCreator);
        assertNotNull(obsInstances);

        Field orderField = bundleCreator.getClass().getDeclaredField("orderResourceHandlers");
        orderField.setAccessible(true);
        Object orderInstances = orderField.get(bundleCreator);
        assertNotNull(orderInstances);
=======
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
>>>>>>> BDSHR-206 | adding interceptor to saveEncounter
    }

    @Test
    public void shouldCreateFhirBundle() throws Exception {
        executeDataSet("shrClientBundleCreatorTestDS.xml");
        AtomFeed bundle = bundleCreator.compose(Context.getEncounterService().getEncounter(36));
        assertNotNull(bundle);
    }
}
