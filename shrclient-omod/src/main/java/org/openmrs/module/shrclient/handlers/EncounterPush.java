package org.openmrs.module.shrclient.handlers;

import ca.uhn.fhir.model.dstu2.resource.BaseResource;
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
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.bundler.CompositionBundleCreator;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.model.*;
import org.openmrs.module.shrclient.util.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EncounterPush implements EventWorker {

    private static final Logger log = Logger.getLogger(EncounterPush.class);
    private IdMappingRepository idMappingsRepository;
    private CompositionBundleCreator compositionBundleCreator;
    private EncounterService encounterService;
    private PropertiesReader propertiesReader;
    private ClientRegistry clientRegistry;
    private SHRClient shrClient;
    private List<String> encounterUuidsProcessed;
    private SystemUserService systemUserService;

    public EncounterPush(EncounterService encounterService, PropertiesReader propertiesReader,
                         CompositionBundleCreator compositionBundleCreator, IdMappingRepository idMappingsRepository,
                         ClientRegistry clientRegistry,
                         SystemUserService systemUserService) throws IdentityUnauthorizedException {
        this.encounterService = encounterService;
        this.propertiesReader = propertiesReader;
        this.clientRegistry = clientRegistry;
        this.systemUserService = systemUserService;
        this.shrClient = clientRegistry.getSHRClient();
        this.compositionBundleCreator = compositionBundleCreator;
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
            saveEncounterIdMapping(openMrsEncounter.getUuid(), healthId, shrEncounterId, systemProperties);
            saveOrderIdMapping(openMrsEncounter.getOrders(), healthId, shrEncounterId, systemProperties);
        } catch (Exception e) {
            log.error("Error while processing encounter sync event.", e);
            throw new RuntimeException(e);
        }
    }

    private void saveOrderIdMapping(Set<Order> orders, String healthId, String shrEncounterId, SystemProperties systemProperties) {
        HashMap<String, String> orderUrlReferenceIds = new HashMap<>();
        orderUrlReferenceIds.put(EntityReference.HEALTH_ID_REFERENCE, healthId);
        orderUrlReferenceIds.put(EntityReference.ENCOUNTER_ID_REFERENCE, shrEncounterId);
        orderUrlReferenceIds.put(EntityReference.REFERENCE_RESOURCE_NAME, new MedicationOrder().getResourceName());
        EntityReference entityReference = new EntityReference();
        for (Order order : orders) {
            if (order.getOrderType().getName().equals(MRSProperties.MRS_DRUG_ORDER_TYPE)) {
                orderUrlReferenceIds.remove(EntityReference.REFERENCE_ID);
                orderUrlReferenceIds.put(EntityReference.REFERENCE_ID, order.getUuid());
                String orderUrl = entityReference.build(BaseResource.class, systemProperties, orderUrlReferenceIds);
                String externalId = String.format(MRSProperties.RESOURCE_MAPPING_EXTERNAL_ID_FORMAT, shrEncounterId, order.getUuid());
                idMappingsRepository.saveOrUpdateIdMapping(new MedicationOrderIdMapping(order.getUuid(), externalId, orderUrl));
            }
        }
    }

    private void saveEncounterIdMapping(String openMrsEncounterUuid, String healthId, String shrEncounterId, SystemProperties systemProperties) {
        String shrEncounterUrl = getShrEncounterUrl(healthId, shrEncounterId, systemProperties);
        idMappingsRepository.saveOrUpdateIdMapping(new EncounterIdMapping(openMrsEncounterUuid, shrEncounterId, shrEncounterUrl, new Date()));
    }

    private String getShrEncounterUrl(String healthId, String shrEncounterId, SystemProperties systemProperties) {
        HashMap<String, String> encounterUrlReferenceIds1 = new HashMap<>();
        encounterUrlReferenceIds1.put(EntityReference.HEALTH_ID_REFERENCE, healthId);
        encounterUrlReferenceIds1.put(EntityReference.REFERENCE_ID, shrEncounterId);
        HashMap<String, String> encounterUrlReferenceIds = encounterUrlReferenceIds1;
        return new EntityReference().build(Encounter.class, systemProperties, encounterUrlReferenceIds);
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
                    compositionBundleCreator.create(openMrsEncounter, healthId, systemProperties));
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
                    compositionBundleCreator.create(openMrsEncounter, healthId, systemProperties));
        } catch (IdentityUnauthorizedException e) {
            log.error("Clearing unauthorized identity token.");
            clientRegistry.clearIdentityToken();
            throw e;
        }
    }

    private String getEncounterIdFromResponse(String shrEncounterResponse) throws java.io.IOException {
        EncounterResponse encounterResponse = configureObjectMapper().readValue(shrEncounterResponse,
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
