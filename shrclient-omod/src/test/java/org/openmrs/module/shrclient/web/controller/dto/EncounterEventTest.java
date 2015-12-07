package org.openmrs.module.shrclient.web.controller.dto;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EncounterEventTest {

    @Test
    public void shouldDeSerializeEncounterEvent() throws IOException {
        final URL resource = URLClassLoader.getSystemResource("sample_encounter_bundle.json");
        final String json = FileUtils.readFileToString(new File(resource.getPath()));
        ObjectMapper mapper = new ObjectMapper();
        List<EncounterEvent> events = mapper.readValue(json, new TypeReference<List<EncounterEvent>>() {});
        assertEquals(1, events.size());

        EncounterEvent encounterEvent = events.get(0);
        assertNotNull(encounterEvent.getEncounterId());
        assertNotNull(encounterEvent.getHealthId());

        final Bundle bundle = encounterEvent.getBundle();
        assertEquals("Bundle/4fe6f9e2-d10a-4956-aae5-091e810090e1", bundle.getId().getValue());
        assertNotNull(bundle);
        assertNotNull(bundle.getEntry());
        assertEquals(2, bundle.getEntry().size());
        assertEquals(new Composition().getResourceName(), bundle.getEntry().get(0).getResource().getResourceName());
        assertEquals(new Encounter().getResourceName(), bundle.getEntry().get(1).getResource().getResourceName());
    }
}
