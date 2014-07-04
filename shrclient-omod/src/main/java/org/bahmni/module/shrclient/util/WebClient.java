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

public class WebClient {
    private static final Logger log = Logger.getLogger(WebClient.class);
    private ObjectMapper mapper = new ObjectMapper();
    private String user;
    private String password;
    private String baseUrl;

    public WebClient(String user, String password, String host, String port) {
        this.user = user;
        this.password = password;
        this.baseUrl = String.format("http://%s:%s", host, port);
    }

    public <T> T get(String url, Class<T> returnType) {
        url = getUrl(url);
        log.debug("HTTP get url: " + url);
        HttpGet request = new HttpGet(URI.create(url));
        request.addHeader("accept", "application/json");

        try {
            String response = getResponse(request);
            if (StringUtils.isNotBlank(response)) {
                return mapper.readValue(response, returnType);
            }
        } catch (IOException e) {
            log.error("Error during http get. URL: " + url, e);
            throw new RuntimeException(e);
        }
        return null;
    }

    public String post(String url, Object data) {
        url = getUrl(url);
        log.debug("HTTP post url: " + url);
        HttpPost request = new HttpPost(URI.create(url));

        try {
            StringEntity entity = new StringEntity(mapper.writeValueAsString(data));
            entity.setContentType("application/json");
            request.setEntity(entity);

            return getResponse(request);
        } catch (IOException e) {
            log.error("Error during http post. URL: " + url, e);
            throw new RuntimeException(e);
        }
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

    private String getUrl(String url) {
        return baseUrl + url;
    }

    String getAuthHeader() {
        String auth = user + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("UTF-8")));
        return "Basic " + new String(encodedAuth);
    }
}
