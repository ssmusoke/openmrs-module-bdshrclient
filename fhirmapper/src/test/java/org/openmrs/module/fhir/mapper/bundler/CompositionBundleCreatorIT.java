package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.AtomFeed;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir.TestHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;


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
        executeDataSet("testDataSets/shrClientBundleCreatorTestDS.xml");
        String facilityId = "10000036";
        AtomFeed bundle = compositionBundle.create(Context.getEncounterService().getEncounter(36), TestHelper.getSystemProperties(facilityId));
        assertNotNull(bundle);
    }

}
