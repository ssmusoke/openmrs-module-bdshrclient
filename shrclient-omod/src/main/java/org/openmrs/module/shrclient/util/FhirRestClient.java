package org.openmrs.module.shrclient.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.instance.formats.XmlComposer;
import org.hl7.fhir.instance.model.AtomFeed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class FhirRestClient {

    private static final Logger log = Logger.getLogger(RestClient.class);

    private final WebClient webClient;
    private final ObjectMapper mapper;

    public FhirRestClient(String baseUrl, String user, String password) {
        webClient = new WebClient(baseUrl, user, password);
        mapper = new ObjectMapper();
    }

    public <T> T get(String url, TypeReference valueTypeRef) {
        try {
            String response = webClient.get(url);
            if (StringUtils.isNotBlank(response)) {
                return mapper.readValue(response, valueTypeRef);
            } else {
                return null;
            }
        } catch (IOException e) {
            log.error("Error Mapping response to : " + url, e);
            throw new RuntimeException(e);
        }
    }

    public String post(String url, AtomFeed bundle) {
        try {
            XmlComposer composer = new XmlComposer();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            composer.compose(byteArrayOutputStream, bundle, true);
            log.error(String.format("Posting data %s to url %s", bundle, url));
            log.info(String.format("Posting data %s to url %s", bundle, url));
            log.debug(String.format("Posting data %s to url %s", bundle, url));
            return webClient.post(url, byteArrayOutputStream.toString(), "application/xml;charset=UTF-8");
        } catch (Exception e) {
            log.error("Error during http post. URL: " + url, e);
            throw new RuntimeException(e);
        }
    }
}
