package org.bahmni.module.shrclient.util;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

public class MciWebClient {
    private static final Logger log = Logger.getLogger(MciWebClient.class);

    private ObjectMapper jsonMapper = new ObjectMapper();
    private FreeShrClientProperties properties = new FreeShrClientProperties();

    public <T> T get(String url, Class<T> returnType) throws IOException {
        log.debug("HTTP get url: " + url);
        HttpGet request = new HttpGet(URI.create(url));
        request.addHeader("accept", "application/json");

        String response = getResponse(request);
        if (StringUtils.isNotBlank(response)) {
            return jsonMapper.readValue(response, returnType);
        }
        return null;
    }

    public String post(String url, Object data) throws IOException {
        log.debug("HTTP post url: " + url);
        HttpPost request = new HttpPost(URI.create(url));
        StringEntity entity = new StringEntity(jsonMapper.writeValueAsString(data));
        entity.setContentType("application/json");
        request.setEntity(entity);

        return getResponse(request);
    }

    private String getResponse(HttpRequestBase request) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            request.addHeader("Authorization", getAuthHeader());

            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                public String handleResponse(final HttpResponse response) throws IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }
            };
            return httpClient.execute(request, responseHandler);

        } finally {
            httpClient.close();
        }
    }

    String getAuthHeader() throws IOException {
        String auth = properties.getMciUser() + ":" + properties.getMciPassword();
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("UTF-8")));
        return "Basic " + new String(encodedAuth);
    }
}
