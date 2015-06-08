package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Composition;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;


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
        AtomFeed bundle = compositionBundle.create(Context.getEncounterService().getEncounter(36), getSystemProperties(facilityId));
        assertNotNull(bundle);
    }

    @Test
    public void shouldPopulateAuthorFromServiceProvider() throws Exception {
        executeDataSet("testDataSets/shrClientBundleCreatorTestDS.xml");
        AtomFeed bundle = compositionBundle.create(Context.getEncounterService().getEncounter(36), getSystemProperties("12345"));
        assertNotNull(bundle);
        Composition composition = FHIRFeedHelper.getComposition(bundle);
        assertEquals("http://hrmtest.dghs.gov.bd/api/1.0/facilities/12345.json" ,composition.getAuthor().get(0).getReferenceSimple());
        assertEquals("http://hrmtest.dghs.gov.bd/api/1.0/facilities/12345.json" ,bundle.getAuthorUri());
    }
}
