package org.openmrs.module.shrclient.feeds.shr;

import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.ict4h.atomfeed.client.repository.AllFeeds;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Map;

public class ShrEncounterFeeds extends AllFeeds {
    private Map<String, String> feedHeaders;

    public ShrEncounterFeeds(Map<String, String> feedHeaders) {
        this.feedHeaders = feedHeaders;
    }

    @Override
    public Feed getFor(URI uri) {
        HttpGet request = new HttpGet(uri);
        request.setHeader("Accept", getAcceptHeader());
        request.setHeader("facilityId", getFacilityId());
        try {
            String response = execute(request);
            //works only for application/atom+xml
            WireFeedInput input = new WireFeedInput();
            return (Feed) input.build(new StringReader(response));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FeedException e) {
            e.printStackTrace();
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
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else if (status == 404) {
                        return null;
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
}
