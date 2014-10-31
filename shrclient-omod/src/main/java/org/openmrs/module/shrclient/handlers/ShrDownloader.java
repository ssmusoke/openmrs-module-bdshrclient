package org.openmrs.module.shrclient.handlers;


import org.apache.log4j.Logger;
import org.openmrs.api.PatientService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.feeds.shr.DefaultEncounterFeedWorker;
import org.openmrs.module.shrclient.feeds.shr.ShrEncounterFeedProcessor;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ShrDownloader {
    private final Logger logger = Logger.getLogger(ShrDownloader.class);

    public void download() {
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        ArrayList<String> encounterFeedUrls = getEncounterFeedUrls(propertiesReader);
        Map<String, String> requestProperties = getRequestProperties();
        DefaultEncounterFeedWorker defaultEncounterFeedWorker = getEncounterFeedWorker();
        for (String encounterFeedUrl : encounterFeedUrls) {
            ShrEncounterFeedProcessor feedProcessor =
               new ShrEncounterFeedProcessor(encounterFeedUrl, requestProperties, defaultEncounterFeedWorker);
            try {
                feedProcessor.process();
            } catch (URISyntaxException e) {
                logger.error("Couldn't download catchment encounters. Error: ", e);
            }
        }
    }

    private DefaultEncounterFeedWorker getEncounterFeedWorker() {
        MciPatientService mciPatientService = PlatformUtil.getRegisteredComponent(MciPatientService.class);
        PatientService patientService = PlatformUtil.getRegisteredComponent(PatientService.class);
        IdMappingsRepository idMappingsRepository = PlatformUtil.getRegisteredComponent(IdMappingsRepository.class);
        PropertiesReader propertiesReader = PlatformUtil.getRegisteredComponent(PropertiesReader.class);
        return new DefaultEncounterFeedWorker(mciPatientService, patientService, idMappingsRepository, propertiesReader);
    }

    private HashMap<String, String> getRequestProperties() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/atom+xml");
        //read from headers or application
        headers.put("facilityId", getFacilityId());
        return headers;
    }

    private String getFacilityId() {
        return "10000069";
    }

    public ArrayList<String> getEncounterFeedUrls(PropertiesReader propertiesReader) {
        Properties shrProperties = propertiesReader.getShrProperties();
        String shrBaseUrl = propertiesReader.getShrBaseUrl(shrProperties);
        String catchments = (String) shrProperties.get("shr.catchments");
        String[] facilityCatchments = catchments.split(",");
        ArrayList<String> catchmentsUrls = new ArrayList<>();
        for (String facilityCatchment : facilityCatchments) {
            catchmentsUrls.add(String.format("%s/catchments/%s/encounters", shrBaseUrl, facilityCatchment));
        }
        return catchmentsUrls;
    }

    public void retry() {
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        ArrayList<String> encounterFeedUrls = getEncounterFeedUrls(propertiesReader);

        Map<String, String> requestProperties = getRequestProperties();
        DefaultEncounterFeedWorker defaultEncounterFeedWorker = getEncounterFeedWorker();
        for (String encounterFeedUrl : encounterFeedUrls) {
            ShrEncounterFeedProcessor feedProcessor =
                    new ShrEncounterFeedProcessor(encounterFeedUrl, requestProperties, defaultEncounterFeedWorker);
            try {
                feedProcessor.processFailedEvents();
            } catch (URISyntaxException e) {
                logger.error("Couldn't download catchment encounters. Error: ", e);
            }
        }
    }
}
