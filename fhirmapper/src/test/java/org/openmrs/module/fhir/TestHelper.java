package org.openmrs.module.fhir;

import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.formats.XmlParser;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Properties;

public class TestHelper {
    public ParserBase.ResourceOrFeed loadSampleFHIREncounter(String filePath, ApplicationContext springContext) throws Exception {
        org.springframework.core.io.Resource resource = springContext.getResource(filePath);
        ParserBase.ResourceOrFeed parsedResource =
                new XmlParser().parseGeneral(resource.getInputStream());
        return parsedResource;
    }

    public static SystemProperties getSystemProperties(String facilityId) {
        Properties shrProperties = new Properties();
        shrProperties.setProperty(SystemProperties.FACILITY_ID, facilityId);

        Properties frProperties = new Properties();
        frProperties.setProperty(SystemProperties.FACILITY_URL_FORMAT, "%s.json");

        Properties trProperties = new Properties();
        trProperties.setProperty("tr.base.valuset.url", "openmrs/ws/rest/v1/tr/vs");
        trProperties.setProperty("tr.valueset.route", "sample-route");
        trProperties.setProperty("tr.valueset.quantityunits", "sample-units");

        HashMap<String, String> baseUrls = new HashMap<>();
        baseUrls.put("mci", "http://mci");
        baseUrls.put("tr", "http://tr");
        return new SystemProperties(baseUrls, shrProperties, frProperties, trProperties);
    }
}
