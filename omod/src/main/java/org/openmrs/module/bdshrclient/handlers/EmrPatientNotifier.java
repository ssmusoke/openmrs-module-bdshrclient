package org.openmrs.module.bdshrclient.handlers;


import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.ict4h.atomfeed.client.service.FeedClient;
import org.ict4h.atomfeed.transaction.AFTransactionManager;
import org.openmrs.module.bdshrclient.OpenMRSFeedClientFactory;
import org.springframework.transaction.PlatformTransactionManager;


import java.net.URI;
import java.net.URISyntaxException;

public class EmrPatientNotifier {

    private AFTransactionManager txMgr;
    private final String openmrsPatientFeedUri = "openmrs://events/patient/recent";

    public void process() {
        OpenMRSFeedClientFactory factory = new OpenMRSFeedClientFactory();
        FeedClient feedClient = null;
        try {
            feedClient = factory.getFeedClient(getEmrPatientUpdateUri(), new ShrPatientCreator());
            feedClient.processEvents();
        } catch (URISyntaxException e) {
            //TODO replace with logger
            System.out.println("Invalid URI : " + e);
        }
    }


    private class ShrPatientCreator implements EventWorker {
        @Override
        public void process(Event event) {
            System.out.println("Hello World! Here's what I processing:" + event);
        }

        @Override
        public void cleanUp(Event event) {
            System.out.println("Hello World! cleaning up events:" + event);
        }
    }


    private URI getEmrPatientUpdateUri() throws URISyntaxException {
        return new URI(openmrsPatientFeedUri);
    }





}
