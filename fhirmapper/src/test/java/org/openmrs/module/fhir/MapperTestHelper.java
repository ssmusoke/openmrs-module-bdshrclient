package org.openmrs.module.fhir;

import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.formats.XmlParser;
import org.openmrs.module.fhir.utils.PropertyKeyConstants;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Properties;

import static org.openmrs.module.fhir.utils.PropertyKeyConstants.FACILITY_ID;
import static org.openmrs.module.fhir.utils.PropertyKeyConstants.FACILITY_REFERENCE_PATH;

public class MapperTestHelper {
    public ParserBase.ResourceOrFeed loadSampleFHIREncounter(String filePath, ApplicationContext springContext) throws Exception {
        org.springframework.core.io.Resource resource = springContext.getResource(filePath);
        ParserBase.ResourceOrFeed parsedResource =
                new XmlParser().parseGeneral(resource.getInputStream());
        return parsedResource;
    }

    public static SystemProperties getSystemProperties(String facilityId) {
        Properties shrProperties = new Properties();
        shrProperties.setProperty(FACILITY_ID, facilityId);

        Properties facilityRegistry = new Properties();
        facilityRegistry.setProperty(FACILITY_REFERENCE_PATH, "http://hrmtest.dghs.gov.bd/api/1.0/facilities");
        //facilityRegistry.setProperty(FACILITY_URL_FORMAT, "%s.json");

        Properties trProperties = new Properties();
        trProperties.setProperty(PropertyKeyConstants.TR_VALUESET_PATH_INFO, "openmrs/ws/rest/v1/tr/vs");
        trProperties.setProperty(PropertyKeyConstants.TR_VALUESET_ROUTE, "Route-of-Administration");
        trProperties.setProperty(PropertyKeyConstants.TR_VALUESET_QTY_UNITS, "Quantity-Units");
        trProperties.setProperty(PropertyKeyConstants.TR_VALUESET_IMMUNIZATION_REASON, "Immunization-Reason");
        trProperties.setProperty(PropertyKeyConstants.TR_VALUESET_REFUSAL_REASON, "No-Immunization-Reason");

        Properties providerRegistry = new Properties();
        providerRegistry.setProperty(PropertyKeyConstants.PROVIDER_REFERENCE_PATH, "http://hrmtest.dghs.gov.bd/api/1.0/providers");

        Properties facilityInstanceProperties = new Properties();

        HashMap<String, String> baseUrls = new HashMap<>();
        baseUrls.put("mci", "http://mci");
        baseUrls.put("tr", "http://tr");

        Properties mciProperties = new Properties();
        mciProperties.put(PropertyKeyConstants.MCI_REFERENCE_PATH, "http://public.com/");
        mciProperties.put(PropertyKeyConstants.MCI_PATIENT_CONTEXT, "/api/v1/patients");

        return new SystemProperties(baseUrls, facilityRegistry, trProperties, providerRegistry, facilityInstanceProperties, mciProperties);
    }
}
