package org.openmrs.module.shrclient.feeds.shr;


import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.formats.XmlParser;
import org.ict4h.atomfeed.client.AtomFeedProperties;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.repository.AllFailedEvents;
import org.ict4h.atomfeed.client.repository.AllMarkers;
import org.ict4h.atomfeed.client.service.AtomFeedClient;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.openmrs.module.shrclient.feeds.shr.EncounterEventWorker;
import org.openmrs.module.shrclient.feeds.shr.ShrEncounterFeeds;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class ShrEncounterFeedProcessor {

    private PlatformTransactionManager transactionManager;
    private EncounterEventWorker shrEventWorker;
    private String feedUrl;
    private AllMarkers markers;
    private AllFailedEvents failedEvents;
    private Map<String, Object> feedProperties;

    public ShrEncounterFeedProcessor(DataSourceTransactionManager transactionManager,
                                     EncounterEventWorker shrEventWorker,
                                     String feedUrl,
                                     AllMarkers markers,
                                     AllFailedEvents failedEvents,
                                     Map<String, Object> feedProperties) {
        this.transactionManager = transactionManager;
        this.shrEventWorker = shrEventWorker;
        this.feedUrl = feedUrl;
        this.markers = markers;
        this.failedEvents = failedEvents;
        this.feedProperties = feedProperties;
    }

    public void process() throws URISyntaxException {
        AtomFeedProperties atomProperties = new AtomFeedProperties();
        atomProperties.setMaxFailedEvents(20);
        atomFeedClient(new URI(this.feedUrl),
                new FeedEventWorker(shrEventWorker),
                atomProperties).processEvents();
    }

    private AtomFeedClient atomFeedClient(URI feedUri, EventWorker worker, AtomFeedProperties atomProperties)  {
        AtomFeedSpringTransactionManager txManager = new AtomFeedSpringTransactionManager(transactionManager);
        return new AtomFeedClient(
                new ShrEncounterFeeds(feedProperties),
                markers,
                failedEvents,
                atomProperties,
                txManager,
                feedUri,
                worker);
    }

    private class FeedEventWorker implements EventWorker {
        private EncounterEventWorker shrEventWorker;
        FeedEventWorker(EncounterEventWorker shrEventWorker) {
            this.shrEventWorker = shrEventWorker;
        }

        @Override
        public void process(Event event) {
            String content = event.getContent();
            ParserBase.ResourceOrFeed resource;
            try {
                resource = new XmlParser(true).parseGeneral(new ByteArrayInputStream(content.getBytes()));
            } catch (Exception e) {
                throw new RuntimeException("Unable to parse XML", e);
            }
            EncounterBundle encounterBundle = new EncounterBundle();
            encounterBundle.setEncounterId(event.getId());
            encounterBundle.setTitle(event.getTitle());
            encounterBundle.addContent(resource);
            shrEventWorker.process(encounterBundle);
        }

        @Override
        public void cleanUp(Event event) {
        }
    }
}