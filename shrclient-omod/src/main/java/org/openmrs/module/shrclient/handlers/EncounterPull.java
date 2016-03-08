package org.openmrs.module.shrclient.handlers;


import org.apache.log4j.Logger;
import org.openmrs.module.fhir.utils.PropertyKeyConstants;
import org.openmrs.module.shrclient.feeds.shr.DefaultEncounterFeedWorker;
import org.openmrs.module.shrclient.feeds.shr.ShrEncounterFeedProcessor;
import org.openmrs.module.shrclient.identity.IdentityStore;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.service.EMREncounterService;
import org.openmrs.module.shrclient.service.EMRPatientService;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.StringUtil;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.openmrs.module.fhir.utils.PropertyKeyConstants.FACILITY_ID;
import static org.openmrs.module.shrclient.util.Headers.ACCEPT_HEADER_KEY;
import static org.openmrs.module.shrclient.util.Headers.getHrmAccessTokenHeaders;
import static org.springframework.http.MediaType.APPLICATION_ATOM_XML;

public class EncounterPull {
    private final static String FACILITY_ID_HEADER_KEY = "facilityId";

    private final Logger logger = Logger.getLogger(EncounterPull.class);
    private PropertiesReader propertiesReader;
    private IdentityStore identityStore;
    private ClientRegistry clientRegistry;

    public EncounterPull(PropertiesReader propertiesReader, IdentityStore identityStore) {
        this.propertiesReader = propertiesReader;
        this.identityStore = identityStore;
        clientRegistry = new ClientRegistry(propertiesReader, identityStore);
    }

    public void download() {
        ArrayList<String> encounterFeedUrls = getEncounterFeedUrls(propertiesReader);
        try {
            Map<String, String> requestHeaders = getRequestHeaders(propertiesReader);
            DefaultEncounterFeedWorker defaultEncounterFeedWorker = getEncounterFeedWorker();
            for (String encounterFeedUrl : encounterFeedUrls) {
                ShrEncounterFeedProcessor feedProcessor =
                        new ShrEncounterFeedProcessor(encounterFeedUrl, requestHeaders, defaultEncounterFeedWorker,
                                clientRegistry, propertiesReader);
                try {
                    feedProcessor.process();
                } catch (URISyntaxException e) {
                    logger.error("Couldn't download catchment encounters. Error: ", e);
                }
            }
        } catch (IdentityUnauthorizedException e) {
            logger.info("Clearing unauthorized identity token.");
            identityStore.clearToken();
        }
    }

    private DefaultEncounterFeedWorker getEncounterFeedWorker() {
        EMRPatientService emrPatientService = PlatformUtil.getRegisteredComponent("hieEmrPatientService", EMRPatientService.class);
        EMREncounterService emrEncounterService = PlatformUtil.getRegisteredComponent("hieEmrEncounterService", EMREncounterService.class);
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        return new DefaultEncounterFeedWorker(emrPatientService, emrEncounterService, propertiesReader, clientRegistry);
    }

    private HashMap<String, String> getRequestHeaders(PropertiesReader propertiesReader) throws IdentityUnauthorizedException {
        HashMap<String, String> headers = new HashMap<>();
        Properties facilityInstanceProperties = propertiesReader.getFacilityInstanceProperties();
        headers.putAll(getHrmAccessTokenHeaders(clientRegistry.getOrCreateIdentityToken(), facilityInstanceProperties));
        headers.put(ACCEPT_HEADER_KEY, APPLICATION_ATOM_XML.toString());
        headers.put(FACILITY_ID_HEADER_KEY, getFacilityId(facilityInstanceProperties));
        return headers;
    }

    private String getFacilityId(Properties facilityInstanceProperties) {
        Object facilityId = facilityInstanceProperties.getProperty(FACILITY_ID);
        logger.info("Identified Facility:" + facilityId);
        if (facilityId == null) {
            throw new RuntimeException("Facility Id not defined.");
        }
        return (String) facilityId;
    }

    public ArrayList<String> getEncounterFeedUrls(PropertiesReader propertiesReader) {
        Properties facilityInstanceProperties = propertiesReader.getFacilityInstanceProperties();
        String shrBaseUrl = StringUtil.ensureSuffix(propertiesReader.getShrBaseUrl(), "/");
        String catchmentPathPattern = StringUtil.removePrefix(propertiesReader.getShrCatchmentPathPattern(), "/");
        String catchments = facilityInstanceProperties.get(PropertyKeyConstants.FACILITY_CATCHMENTS).toString();
        String[] facilityCatchments = catchments.split(",");
        ArrayList<String> catchmentsUrls = new ArrayList<>();
        for (String facilityCatchment : facilityCatchments) {
            String catchmentUrl = shrBaseUrl + String.format(catchmentPathPattern, facilityCatchment);
            //catchmentsUrls.add(String.format("%s/catchments/%s/encounters", shrBaseUrl, facilityCatchment));
            catchmentsUrls.add(catchmentUrl);
        }
        return catchmentsUrls;
    }

    public void retry() {
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        ArrayList<String> encounterFeedUrls = getEncounterFeedUrls(propertiesReader);
        try {
            Map<String, String> requestProperties = getRequestHeaders(propertiesReader);
            DefaultEncounterFeedWorker defaultEncounterFeedWorker = getEncounterFeedWorker();
            for (String encounterFeedUrl : encounterFeedUrls) {
                ShrEncounterFeedProcessor feedProcessor =
                        new ShrEncounterFeedProcessor(encounterFeedUrl, requestProperties, defaultEncounterFeedWorker,
                                clientRegistry, propertiesReader);
                try {
                    feedProcessor.processFailedEvents();
                } catch (URISyntaxException e) {
                    logger.error("Couldn't download catchment encounters. Error: ", e);
                }
            }
        } catch (IdentityUnauthorizedException e) {
            logger.info("Clearing unauthorized identity token.");
            identityStore.clearToken();
        }
    }
}
