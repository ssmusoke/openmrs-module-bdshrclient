package org.openmrs.module.bdshrclient.handlers;

import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedOutput;
import org.ict4h.atomfeed.client.repository.AllFeeds;
import org.ict4h.atomfeed.server.service.EventFeedService;

import java.net.URI;
import java.net.URISyntaxException;

public class OpenMRSFeeds extends AllFeeds {

    private EventFeedService eventFeedService;
    private URI emrPatientUpdateUri;

    public OpenMRSFeeds(EventFeedService eventFeedService, URI emrPatientUpdateUri) {
        this.eventFeedService = eventFeedService;
        this.emrPatientUpdateUri = emrPatientUpdateUri;
    }

    @Override
    public Feed getFor(URI uri) {
        //TODO
        System.out.println("fetching feed for:" + uri);

        String eventUrlPath = uri.getPath();
        if (eventUrlPath.startsWith("/")) {
            eventUrlPath = eventUrlPath.substring(1, eventUrlPath.length());
        }
        if (eventUrlPath.endsWith("/")) {
            eventUrlPath = eventUrlPath.substring(0, eventUrlPath.length()-1);
        }
        String[] pathInfo = eventUrlPath.split("/");
        String category = pathInfo[0];
        String feedId =  (pathInfo.length > 1) ? pathInfo[1] : "recent";

        Feed responseFeed;
        if ("recent".equals(feedId)) {
            responseFeed = eventFeedService.getRecentFeed(uri, category);
        } else {
            responseFeed = eventFeedService.getEventFeed(uri, category, Integer.valueOf(feedId));
        }
        responseFeed.setOtherLinks(responseFeed.getAlternateLinks());
        return responseFeed;

    }


}
