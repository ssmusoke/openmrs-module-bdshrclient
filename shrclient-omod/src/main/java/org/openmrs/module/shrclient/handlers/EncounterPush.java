package org.openmrs.module.shrclient.handlers;

import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.exceptions.AtomFeedClientException;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.module.fhir.Constants;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.bundler.CompositionBundle;
import org.openmrs.module.fhir.utils.SHREncounterURLUtil;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.model.*;
import org.openmrs.module.shrclient.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EncounterPush implements EventWorker {

    private static final Logger log = Logger.getLogger(EncounterPush.class);
    private IdMappingRepository idMappingsRepository;
    private CompositionBundle compositionBundle;
    private EncounterService encounterService;
    private PropertiesReader propertiesReader;
    private ClientRegistry clientRegistry;
    private SHRClient shrClient;
    private List<String> encounterUuidsProcessed;
    private SystemUserService systemUserService;

    public EncounterPush(EncounterService encounterService, PropertiesReader propertiesReader,
                         CompositionBundle compositionBundle, IdMappingRepository idMappingsRepository,
                         ClientRegistry clientRegistry,
                         SystemUserService systemUserService) throws IdentityUnauthorizedException {
        this.encounterService = encounterService;
        this.propertiesReader = propertiesReader;
        this.clientRegistry = clientRegistry;
        this.systemUserService = systemUserService;
        this.shrClient = clientRegistry.getSHRClient();
        this.compositionBundle = compositionBundle;
        this.idMappingsRepository = idMappingsRepository;
        this.encounterUuidsProcessed = new ArrayList<>();
    }

    @Override
    public void process(Event event) {
        log.debug("Event: [" + event + "]");
        try {
            String uuid = getUuid(event.getContent());
            org.openmrs.Encounter openMrsEncounter = encounterService.getEncounterByUuid(uuid);
            if (openMrsEncounter == null) {
                log.debug(String.format("No OpenMRS encounter exists with uuid: [%s].", uuid));
                return;
            }
            IdMapping mapping = getEncounterMapping(openMrsEncounter);
            if (!shouldUploadEncounter(openMrsEncounter)) {
                return;
            }

            String healthId = getPatientHealthId(openMrsEncounter.getPatient());
            log.debug("Uploading patient encounter to SHR : [ " + openMrsEncounter.getUuid() + "]");
            String shrEncounterId = mapping != null ? mapping.getExternalId() : null;
            SystemProperties systemProperties = new SystemProperties(
                    propertiesReader.getFrProperties(),
                    propertiesReader.getTrProperties(),
                    propertiesReader.getPrProperties(),
                    propertiesReader.getFacilityInstanceProperties(),
                    propertiesReader.getMciProperties(),
                    propertiesReader.getShrProperties());
            if (shrEncounterId != null) {
                pushEncounterUpdate(openMrsEncounter, shrEncounterId, healthId, systemProperties);
            } else {
                shrEncounterId = pushEncounterCreate(openMrsEncounter, healthId, systemProperties);
            }
            encounterUuidsProcessed.add(openMrsEncounter.getUuid());
            String encounterUrl = SHREncounterURLUtil.getEncounterUrl(shrEncounterId, healthId, systemProperties);
            saveEncounterIdMapping(openMrsEncounter, shrEncounterId, encounterUrl);
            saveOrderIdMapping(openMrsEncounter.getOrders(), encounterUrl);
        } catch (Exception e) {
            log.error("Error while processing encounter sync event.", e);
            throw new RuntimeException(e);
        }
    }

    private void saveOrderIdMapping(Set<Order> orders, String encounterUrl) {
        for (Order order : orders) {
            if (order.getOrderType().getName().equals(MRSProperties.MRS_DRUG_ORDER_TYPE)) {
                String orderUrl = String.format(Constants.RESOURCE_MAPPING_URL_FORMAT, encounterUrl, new MedicationOrder().getResourceName(), order.getUuid());
                idMappingsRepository.saveOrUpdateIdMapping(new IdMapping(order.getUuid(), order.getUuid(), IdMappingType.MEDICATION_ORDER, orderUrl));
            }
        }
    }

    private void saveEncounterIdMapping(Encounter openMrsEncounter, String shrEncounterId, String encounterUrl) {
        idMappingsRepository.saveOrUpdateIdMapping(new EncounterIdMapping(openMrsEncounter.getUuid(), shrEncounterId, encounterUrl, new Date()));
    }

    private boolean shouldUploadEncounter(Encounter openMrsEncounter) {
        if (systemUserService.isUpdatedByOpenMRSShrSystemUser(openMrsEncounter)) {
            log.debug(String.format("Encounter downloaded from SHR.Ignoring encounter sync."));
            return false;
        }
        /*
        * To avoid processing of events for the same encounter in a job run, we store the encounter uuids processed in this job run.
        * It is cleared once the job has finished processing.
        */
        if (encounterUuidsProcessed.contains(openMrsEncounter.getUuid())) {
            log.debug(String.format("Enounter[%s] has been processed in the same job run before.", openMrsEncounter.getUuid()));
            return false;
        }
        return true;
    }

    private String pushEncounterCreate(Encounter openMrsEncounter, String healthId, SystemProperties systemProperties) throws IOException {
        try {
            String encPathPattern = StringUtil.removePrefix(propertiesReader.getShrPatientEncPathPattern(), "/");
            String shrEncounterCreateResponse = shrClient.post(String.format(encPathPattern, healthId),
                    compositionBundle.create(openMrsEncounter, healthId, systemProperties));
            return getEncounterIdFromResponse(shrEncounterCreateResponse);
        } catch (IdentityUnauthorizedException e) {
            log.error("Clearing unauthorized identity token.");
            clientRegistry.clearIdentityToken();
            throw e;
        }
    }

    private void pushEncounterUpdate(Encounter openMrsEncounter, String shrEncounterId, String healthId, SystemProperties systemProperties) throws IOException {
        try {
            String encPathPattern = StringUtil.removePrefix(propertiesReader.getShrPatientEncPathPattern(), "/");
            String encPath = String.format(encPathPattern, healthId);
            String encUpdateUrl = String.format("%s/%s", encPath, shrEncounterId);
            shrClient.put(encUpdateUrl,
                    compositionBundle.create(openMrsEncounter, healthId, systemProperties));
        } catch (IdentityUnauthorizedException e) {
            log.error("Clearing unauthorized identity token.");
            clientRegistry.clearIdentityToken();
            throw e;
        }
    }

    private String getEncounterIdFromResponse(String shrEncounterUuid) throws java.io.IOException {
        EncounterResponse encounterResponse = configureObjectMapper().readValue(shrEncounterUuid,
                EncounterResponse.class);
        //TODO : set the right url
        return encounterResponse.getEncounterId();
    }

    private ObjectMapper configureObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    private String getPatientHealthId(Patient emrPatient) {
        PatientIdMapping patientIdMapping = (PatientIdMapping) idMappingsRepository.findByInternalId(emrPatient.getUuid(), IdMappingType.PATIENT);
        if (patientIdMapping == null) {
            throw new AtomFeedClientException(String.format("Patient [%s] is not yet synced to MCI.",
                    emrPatient.getUuid()));
        }

        return patientIdMapping.getExternalId();
    }

    private IdMapping getEncounterMapping(Encounter openMrsEncounter) {
        return idMappingsRepository.findByInternalId(openMrsEncounter.getUuid(), IdMappingType.ENCOUNTER);
    }

    String getUuid(String content) {
        String encounterUuid = null;
        Pattern p = Pattern.compile("^\\/openmrs\\/ws\\/rest\\/v1\\/encounter\\/(.*)\\?v=.*");
        Matcher m = p.matcher(content);
        if (m.matches()) {
            encounterUuid = m.group(1);
        }
        return encounterUuid;
    }

    @Override
    public void cleanUp(Event event) {

    }
}
