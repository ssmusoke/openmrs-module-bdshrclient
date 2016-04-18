package org.openmrs.module.shrclient.feeds.shr;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
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
import org.openmrs.module.shrclient.util.FhirBundleContextHolder;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.web.controller.dto.EncounterEvent;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class ShrEncounterFeedProcessor {

    private EncounterEventWorker shrEventWorker;
    private String feedUrl;
    private Map<String, String> requestHeaders;
    private ClientRegistry clientRegistry;
    private PropertiesReader propertiesReader;

    public ShrEncounterFeedProcessor(String feedUrl,
                                     Map<String, String> requestHeaders, EncounterEventWorker shrEventWorker,
                                     ClientRegistry clientRegistry, PropertiesReader propertiesReader) {
        this.shrEventWorker = shrEventWorker;
        this.feedUrl = feedUrl;
        this.requestHeaders = requestHeaders;
        this.clientRegistry = clientRegistry;
        this.propertiesReader = propertiesReader;
    }

    public void process() throws URISyntaxException {
        atomFeedClient(new URI(this.feedUrl), new FeedEventWorker(shrEventWorker), 
                propertiesReader.getShrMaxFailedEvent()).processEvents();
    }

    public void processFailedEvents() throws URISyntaxException {
        atomFeedClient(new URI(this.feedUrl), new FeedEventWorker(shrEventWorker),
                propertiesReader.getShrMaxFailedEvent()).processFailedEvents();
    }

    private AtomFeedProperties getAtomFeedProperties(int maxFailedEvents) {
        AtomFeedProperties atomProperties = new AtomFeedProperties();
        atomProperties.setMaxFailedEvents(maxFailedEvents);
        atomProperties.setHandleRedirection(true);
        return atomProperties;
    }

    private AtomFeedClient atomFeedClient(URI feedUri, EventWorker worker, int maxFailedEvents) {
        AFTransactionManager txManager = getAtomFeedTransactionManager();
        JdbcConnectionProvider connectionProvider = getConnectionProvider(txManager);
        AtomFeedProperties atomProperties = getAtomFeedProperties(maxFailedEvents);
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
        return new ShrEncounterFeeds(requestHeaders, clientRegistry);
    }

    private class FeedEventWorker implements EventWorker {
        private EncounterEventWorker shrEventWorker;

        FeedEventWorker(EncounterEventWorker shrEventWorker) {
            this.shrEventWorker = shrEventWorker;
        }

        @Override
        public void process(Event event) {
            String content = event.getContent();
            FhirContext fhirContext = FhirBundleContextHolder.getFhirContext();
            Bundle bundle;
            try {
                bundle = fhirContext.newXmlParser().parseResource(Bundle.class, content);
            } catch (Exception e) {
                throw new RuntimeException("Unable to parse XML", e);
            }
            EncounterEvent encounterEvent = new EncounterEvent();
            encounterEvent.setTitle(event.getTitle());
            encounterEvent.addContent(bundle);
            encounterEvent.setCategories(event.getCategories());
            shrEventWorker.process(encounterEvent);
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