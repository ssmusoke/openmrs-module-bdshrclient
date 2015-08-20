package org.openmrs.module.shrclient.web.controller.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.ResourceType;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EncounterBundleTest {

    @Test
    public void shouldDeSerializeEncounterBundle() throws IOException {
        final URL resource = URLClassLoader.getSystemResource("sample_encounter_bundle.json");
        final String json = FileUtils.readFileToString(new File(resource.getPath()));
        ObjectMapper mapper = new ObjectMapper();
        List<EncounterBundle> bundles = mapper.readValue(json, new TypeReference<List<EncounterBundle>>() {
        });
        assertEquals(1, bundles.size());

        EncounterBundle bundle = bundles.get(0);
        assertNotNull(bundle.getEncounterId());
        assertNotNull(bundle.getHealthId());

        final AtomFeed feed = bundle.getBundle();
        assertEquals("urn:38052a8c-c5ad-4821-9e38-b49432a2ccc4", feed.getId());
        assertNotNull(feed);
        assertNotNull(feed.getEntryList());
        assertEquals(2, feed.getEntryList().size());
        assertEquals(ResourceType.Composition, feed.getEntryList().get(0).getResource().getResourceType());
        assertEquals(ResourceType.Encounter, feed.getEntryList().get(1).getResource().getResourceType());
    }
}
