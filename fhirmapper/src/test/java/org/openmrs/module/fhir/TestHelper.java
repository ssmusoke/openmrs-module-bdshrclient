package org.openmrs.module.fhir;

import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.formats.XmlParser;
import org.springframework.context.ApplicationContext;

public class TestHelper {
    public ParserBase.ResourceOrFeed loadSampleFHIREncounter(String filePath, ApplicationContext springContext) throws Exception {
        org.springframework.core.io.Resource resource = springContext.getResource(filePath);
        ParserBase.ResourceOrFeed parsedResource =
                new XmlParser().parseGeneral(resource.getInputStream());
        return parsedResource;
    }
}
