package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.AtomFeed;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;

import static junit.framework.Assert.assertNotNull;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class CompositionBundleCreatorIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    CompositionBundleCreator bundleCreator;

    @Test
    public void shouldWireAllResourceHandlers() throws Exception {
        final Field field = bundleCreator.getClass().getDeclaredField("resourceHandlers");
        field.setAccessible(true);
        Object instances = field.get(bundleCreator);

    }

    @Test
    public void shouldCreateFhirBundle() throws Exception {
        executeDataSet("shrClientBundleCreatorTestDS.xml");
        AtomFeed bundle = bundleCreator.compose(Context.getEncounterService().getEncounter(36));
        assertNotNull(bundle);
    }
}
