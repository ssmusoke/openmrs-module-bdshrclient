package org.openmrs.module.shrclient.handlers;


import org.apache.log4j.Logger;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.ict4h.atomfeed.client.service.FeedClient;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.fhir.mapper.bundler.CompositionBundleCreator;
import org.openmrs.module.shrclient.feeds.openmrs.OpenMRSFeedClientFactory;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.mapper.PatientMapper;
import org.openmrs.module.shrclient.service.impl.BbsCodeServiceImpl;
import org.openmrs.module.shrclient.util.PropertiesReader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class ShrNotifier {

    private static final Logger log = Logger.getLogger(ShrNotifier.class);

    private static final String OPENMRS_PATIENT_FEED_URI = "openmrs://events/patient/recent";
    private static final String OPENMRS_ENCOUNTER_FEED_URI = "openmrs://events/encounter/recent";

    public void processPatient() {
        PatientUploader patientUploader = patientUploader();
        process(OPENMRS_PATIENT_FEED_URI, patientUploader);
    }

    private PatientUploader patientUploader() {
        return new PatientUploader(
                    Context.getPatientService(),
                    Context.getUserService(),
                    Context.getPersonService(),
                    new PatientMapper(Context.getService(AddressHierarchyService.class), new BbsCodeServiceImpl()),
                    getPropertiesReader().getMciWebClient());
    }


    public void processEncounter() {
        process(OPENMRS_ENCOUNTER_FEED_URI, encounterUploader());
    }

    public void retryEncounter() {
        retry(OPENMRS_ENCOUNTER_FEED_URI, encounterUploader());
    }

    public void retryPatient() {
        retry(OPENMRS_PATIENT_FEED_URI, patientUploader());
    }

    private EncounterUploader encounterUploader() {
        return new EncounterUploader(Context.getEncounterService(), Context.getUserService(), getPropertiesReader(),
                getRegisteredComponent(CompositionBundleCreator.class), getRegisteredComponent(IdMappingsRepository.class));
    }

    private <T> T getRegisteredComponent(Class<T> clazz) {
        List<T> registeredComponents = Context.getRegisteredComponents(clazz);
        if (!registeredComponents.isEmpty()) {
            return registeredComponents.get(0);
        }
        return null;
    }

    private FeedClient feedClient(String uri, EventWorker worker) {
        OpenMRSFeedClientFactory factory = new OpenMRSFeedClientFactory();
        try {
            return factory.getFeedClient(new URI(uri), worker);
        } catch (URISyntaxException e) {
            log.error("Invalid URI. ", e);
            throw new RuntimeException(e);
        }
    }

    private void retry(String feedURI, EventWorker eventWorker) {
        feedClient(feedURI, eventWorker).processFailedEvents();
    }

    private void process(String feedURI, EventWorker eventWorker) {
        feedClient(feedURI, eventWorker).processEvents();
    }

    private PropertiesReader getPropertiesReader() {
        return getRegisteredComponent(PropertiesReader.class);
    }

}
