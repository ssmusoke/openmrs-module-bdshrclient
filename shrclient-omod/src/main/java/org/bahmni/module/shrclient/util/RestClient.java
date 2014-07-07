package org.bahmni.module.shrclient.util;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

public class RestClient {

    private static final Logger log = Logger.getLogger(RestClient.class);
    private ObjectMapper mapper = new ObjectMapper();
    private final WebClient webClient;

    public RestClient(String user, String password, String host, String port) {
        webClient = new WebClient(user, password, host, port);
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

    public String post(String url, Object data) {
        try {
            String requestBody = mapper.writeValueAsString(data);
            return webClient.post(url, requestBody);
        } catch (IOException e) {
            log.error("Error during http post. URL: " + url, e);
            throw new RuntimeException(e);
        }
    }

    public String getAuthHeader() {
        return webClient.getAuthHeader();
    }
}
