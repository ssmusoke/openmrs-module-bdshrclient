package org.bahmni.module.shrclient.web.controller.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.model.AtomFeed;
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
        final URL resource = URLClassLoader.getSystemResource("encounters.json");
        final String json = FileUtils.readFileToString(new File(resource.getPath()));
        ObjectMapper mapper = new ObjectMapper();
        List<EncounterBundle> bundles = mapper.readValue(json, new TypeReference<List<EncounterBundle>>() {
        });
        assertEquals(4, bundles.size());

        EncounterBundle bundle = bundles.get(2);
        assertNotNull(bundle.getEncounterId());
        assertNotNull(bundle.getHealthId());

        final ParserBase.ResourceOrFeed resourceOrFeed = bundle.getResourceOrFeed();
        assertNotNull(resourceOrFeed);

        final AtomFeed feed = resourceOrFeed.getFeed();
        assertNotNull(feed);
        assertNotNull(feed.getEntryList());
        assertEquals(4, feed.getEntryList().size());
    }
}
