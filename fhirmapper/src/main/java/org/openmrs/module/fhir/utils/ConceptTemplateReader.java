package org.openmrs.module.fhir.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

@Component
public class ConceptTemplateReader {
    private Properties conceptTemplateProperties;

    public HashMap getConceptProperties(String conceptTemplateKey) {
        Properties templateProperties = null;
        try {
            templateProperties = getConceptTemplateProperties();
            String property = templateProperties.getProperty(conceptTemplateKey);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(property, HashMap.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read from properties file. ", e);
        }
    }

    private Properties getConceptTemplateProperties() throws IOException {
        if (conceptTemplateProperties == null) {
            InputStream input = getClass().getClassLoader().getResourceAsStream("openMRSConceptTemplates.properties");
            conceptTemplateProperties = new Properties();
            conceptTemplateProperties.load(input);
        }
        return conceptTemplateProperties;
    }
}
