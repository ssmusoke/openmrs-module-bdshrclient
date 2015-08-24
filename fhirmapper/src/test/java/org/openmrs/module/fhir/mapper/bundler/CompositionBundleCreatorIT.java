package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import org.junit.After;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir.FhirContextHelper;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;
import static org.openmrs.module.fhir.mapper.FHIRProperties.*;


@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CompositionBundleCreatorIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    CompositionBundle compositionBundle;

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

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
        Bundle bundle = compositionBundle.create(Context.getEncounterService().getEncounter(36), getSystemProperties(facilityId));
        assertNotNull(bundle);
        String bundleXml = FhirContextHelper.getFhirContext().newXmlParser().encodeResourceToString(bundle);
        assertNotNull(bundleXml);
    }

    @Test
    public void shouldPopulateCompositionType() throws Exception {
        executeDataSet("testDataSets/shrClientBundleCreatorTestDS.xml");
        Bundle bundle = compositionBundle.create(Context.getEncounterService().getEncounter(36), getSystemProperties("12345"));
        assertNotNull(bundle);
        Composition composition = FHIRFeedHelper.getComposition(bundle);
        CodingDt type = composition.getType().getCoding().get(0);
        assertEquals(LOINC_CODE_DETAILS_NOTE, type.getCode());
        assertEquals(FHIR_DOC_TYPECODES_URL, type.getSystem());
        assertEquals(LOINC_DETAILS_NOTE_DISPLAY, type.getDisplay());
    }
}
