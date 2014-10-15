package org.openmrs.module.shrclient.util;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

public class RestClient {

    private static final Logger log = Logger.getLogger(RestClient.class);
    private final ObjectMapper mapper;
    private final WebClient webClient;

    public RestClient(String user, String password, String host, String port) {
        webClient = new WebClient(user, password, host, port);
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> T get(String url, Class<T> returnType) {
        try {
            String response = webClient.get(url);
            if (StringUtils.isNotBlank(response)) {
                return mapper.readValue(response, returnType);
            } else {
                return null;
            }
        } catch (IOException e) {
            log.error("Error Mapping response to : " + url, e);
            throw new RuntimeException(e);
        }
    }

    public <T> T post(String url, Object data, Class<T> returnType) {
        try {
            String requestBody = mapper.writeValueAsString(data);
            String response = webClient.post(url, requestBody, "application/json");
            if (StringUtils.isNotBlank(requestBody)) {
                return mapper.readValue(response, returnType);
            }
            return null;
        } catch (IOException e) {
            log.error("Error during http post. URL: " + url, e);
            throw new RuntimeException(e);
        }
    }

    public <T> T put(String url, Object data, Class<T> returnType) {
        try {
            String requestBody = mapper.writeValueAsString(data);
            String response = webClient.put(url, requestBody, "application/json");
            if (StringUtils.isNotBlank(requestBody)) {
                return mapper.readValue(response, returnType);
            }
            return null;
        } catch (IOException e) {
            log.error("Error during http post. URL: " + url, e);
            throw new RuntimeException(e);
        }
    }

    public String getAuthHeader() {
        return webClient.getAuthHeader();
    }
}
