package org.openmrs.module.bdshrclient.handlers;


import org.ict4h.atomfeed.client.service.FeedClient;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.bdshrclient.OpenMRSFeedClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class EmrPatientNotifier {
    private static final Logger logger = LoggerFactory.getLogger(EmrPatientNotifier.class);
    private static final String OPENMRS_PATIENT_FEED_URI = "openmrs://events/patient/recent";

    public void process() {
        OpenMRSFeedClientFactory factory = new OpenMRSFeedClientFactory();
        FeedClient feedClient = null;
        try {
            //TODO: Should only 1 instance of ShrPatientCreator be created ?
            feedClient = factory.getFeedClient(getEmrPatientUpdateUri(), new ShrPatientCreator(
                    Context.getService(AddressHierarchyService.class), Context.getPatientService()));
            feedClient.processEvents();
        } catch (URISyntaxException e) {
            logger.error("Invalid URI. ", e);
        }
    }

    private URI getEmrPatientUpdateUri() throws URISyntaxException {
        return new URI(OPENMRS_PATIENT_FEED_URI);
    }
}
