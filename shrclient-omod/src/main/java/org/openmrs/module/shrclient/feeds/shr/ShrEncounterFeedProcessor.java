package org.openmrs.module.shrclient.feeds.shr;


import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.formats.XmlParser;
import org.ict4h.atomfeed.client.AtomFeedProperties;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.repository.AllFailedEvents;
import org.ict4h.atomfeed.client.repository.AllFeeds;
import org.ict4h.atomfeed.client.repository.AllMarkers;
import org.ict4h.atomfeed.client.repository.jdbc.AllFailedEventsJdbcImpl;
import org.ict4h.atomfeed.client.repository.jdbc.AllMarkersJdbcImpl;
import org.ict4h.atomfeed.client.service.AtomFeedClient;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.ict4h.atomfeed.jdbc.JdbcConnectionProvider;
import org.ict4h.atomfeed.transaction.AFTransactionManager;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.openmrs.module.shrclient.handlers.ClientRegistry;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class ShrEncounterFeedProcessor {

    private EncounterEventWorker shrEventWorker;
    private String feedUrl;
    private Map<String, String> feedProperties;
    private ClientRegistry clientRegistry;

    public ShrEncounterFeedProcessor(String feedUrl,
                                     Map<String, String> feedProperties, EncounterEventWorker shrEventWorker,
                                     ClientRegistry clientRegistry) {
        this.shrEventWorker = shrEventWorker;
        this.feedUrl = feedUrl;
        this.feedProperties = feedProperties;
        this.clientRegistry = clientRegistry;
    }

    public void process() throws URISyntaxException {
        atomFeedClient(new URI(this.feedUrl), new FeedEventWorker(shrEventWorker)).processEvents();
    }

    public void processFailedEvents() throws URISyntaxException {
        atomFeedClient(new URI(this.feedUrl), new FeedEventWorker(shrEventWorker)).processFailedEvents();
    }


    private AtomFeedProperties getAtomFeedProperties() {
        AtomFeedProperties atomProperties = new AtomFeedProperties();
        atomProperties.setMaxFailedEvents(20);
        return atomProperties;
    }

    private AtomFeedClient atomFeedClient(URI feedUri, EventWorker worker) {
        AFTransactionManager txManager = getAtomFeedTransactionManager();
        JdbcConnectionProvider connectionProvider = getConnectionProvider(txManager);
        AtomFeedProperties atomProperties = getAtomFeedProperties();
        return new AtomFeedClient(
                getAllFeeds(clientRegistry),
                getAllMarkers(connectionProvider),
                getAllFailedEvent(connectionProvider),
                atomProperties,
                txManager,
                feedUri,
                worker);
    }

    private AllFailedEvents getAllFailedEvent(JdbcConnectionProvider connectionProvider) {
        return new AllFailedEventsJdbcImpl(connectionProvider);
    }

    private AllMarkers getAllMarkers(JdbcConnectionProvider connectionProvider) {
        return new AllMarkersJdbcImpl(connectionProvider);
    }

    private AllFeeds getAllFeeds(ClientRegistry clientRegistry) {
        return new ShrEncounterFeeds(feedProperties, clientRegistry);
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

    private JdbcConnectionProvider getConnectionProvider(AFTransactionManager txMgr) {
        if (txMgr instanceof AtomFeedSpringTransactionManager) {
            return (AtomFeedSpringTransactionManager) txMgr;
        }
        throw new RuntimeException("Atom Feed TransactionManager should provide a connection provider.");
    }

    private AFTransactionManager getAtomFeedTransactionManager() {
        return new AtomFeedSpringTransactionManager(getSpringPlatformTransactionManager());
    }

    private PlatformTransactionManager getSpringPlatformTransactionManager() {
        List<PlatformTransactionManager> platformTransactionManagers = Context.getRegisteredComponents
                (PlatformTransactionManager.class);
        return platformTransactionManagers.get(0);
    }
}