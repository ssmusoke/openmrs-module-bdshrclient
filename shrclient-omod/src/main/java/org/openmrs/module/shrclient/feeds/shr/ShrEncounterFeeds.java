package org.openmrs.module.shrclient.feeds.shr;

import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.ict4h.atomfeed.client.repository.AllFeeds;
import org.openmrs.module.shrclient.handlers.ClientRegistry;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.util.Headers;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Map;

public class ShrEncounterFeeds extends AllFeeds {
    private Map<String, String> feedHeaders;
    private ClientRegistry clientRegistry;
    private static final Logger logger = Logger.getLogger(ShrEncounterFeeds.class);

    public ShrEncounterFeeds(Map<String, String> feedHeaders, ClientRegistry clientRegistry) {
        this.feedHeaders = feedHeaders;
        this.clientRegistry = clientRegistry;
    }

    @Override
    public Feed getFor(URI uri) {
        HttpGet request = new HttpGet(uri);
        request.setHeader("Accept", getAcceptHeader());
        request.setHeader("facilityId", getFacilityId());
        request.setHeader(Headers.AUTH_TOKEN_KEY, getIdentityHeader());
        try {
            String response = execute(request);
            //works only for application/atom+xml
            WireFeedInput input = new WireFeedInput();
            return (Feed) input.build(new StringReader(response));
        } catch(IdentityUnauthorizedException e){
            logger.error(e);
            clientRegistry.clearIdentityToken();
        }
        catch (IOException | FeedException e) {
            logger.error(e);
        }
        return null;
    }

    private String execute(HttpRequestBase request) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                public String handleResponse(final HttpResponse response) throws IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        return response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : null;
                    } else if (status == HttpStatus.NOT_FOUND.value()) {
                        return null;
                    } else if (status == HttpStatus.UNAUTHORIZED.value()) {
                        throw new IdentityUnauthorizedException("Identity not authorized");
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

    private String getAcceptHeader() {
        return feedHeaders.get("Accept");
    }

    private String getFacilityId() {
        return feedHeaders.get("facilityId");
    }

    public String getIdentityHeader() {
        return feedHeaders.get(Headers.AUTH_TOKEN_KEY);
    }
}
