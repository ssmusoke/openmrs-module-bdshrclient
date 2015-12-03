package org.openmrs.module.shrclient.service;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import com.sun.syndication.feed.atom.Category;
import org.apache.log4j.Logger;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir.mapper.emr.FHIRMapper;
import org.openmrs.module.fhir.mapper.model.Confidentiality;
import org.openmrs.module.fhir.mapper.model.ShrEncounter;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.openmrs.module.shrclient.util.SystemUserService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.openmrs.module.fhir.Constants.ID_MAPPING_ENCOUNTER_TYPE;
import static org.openmrs.module.fhir.Constants.LATEST_UPDATE_CATEGORY_TAG;
import static org.openmrs.module.fhir.mapper.model.Confidentiality.getConfidentiality;
import static org.openmrs.module.fhir.utils.SHREncounterURLUtil.getEncounterUrl;

@Service
public class EMREncounterService {

    private static final Logger logger = Logger.getLogger(EMREncounterService.class);

    private PatientService patientService;
    private IdMappingsRepository idMappingsRepository;
    private PropertiesReader propertiesReader;
    private SystemUserService systemUserService;
    private VisitService visitService;
    private FHIRMapper fhirMapper;
    private OrderService orderService;
    private EMRPatientDeathService patientDeathService;

    @Autowired
    public EMREncounterService(PatientService patientService, IdMappingsRepository idMappingsRepository,
                               PropertiesReader propertiesReader, SystemUserService systemUserService,
                               VisitService visitService, FHIRMapper fhirMapper, OrderService orderService, EMRPatientDeathService patientDeathService) {
        this.patientService = patientService;
        this.idMappingsRepository = idMappingsRepository;
        this.propertiesReader = propertiesReader;
        this.systemUserService = systemUserService;
        this.visitService = visitService;
        this.fhirMapper = fhirMapper;
        this.orderService = orderService;
        this.patientDeathService = patientDeathService;
    }

    public void createOrUpdateEncounters(org.openmrs.Patient emrPatient, List<EncounterBundle> bundles, String healthId) {
        for (EncounterBundle bundle : bundles) {
            try {
                createOrUpdateEncounter(emrPatient, bundle, healthId);
            } catch (Exception e) {
                //TODO do proper handling, write to log API?
                logger.error("error Occurred while trying to process Encounter from SHR.", e);
                throw new RuntimeException(e);
            }
        }
    }

    public void createOrUpdateEncounter(org.openmrs.Patient emrPatient, EncounterBundle encounterBundle, String healthId) throws Exception {
        String shrEncounterId = encounterBundle.getEncounterId();
        Bundle bundle = encounterBundle.getBundle();
        logger.debug(String.format("Processing Encounter feed from SHR for patient[%s] with Encounter ID[%s]", encounterBundle.getHealthId(), shrEncounterId));

        if (!shouldSyncEncounter(shrEncounterId, encounterBundle)) return;
        SystemProperties systemProperties = new SystemProperties(
                propertiesReader.getFrProperties(),
                propertiesReader.getTrProperties(),
                propertiesReader.getPrProperties(),
                propertiesReader.getFacilityInstanceProperties(),
                propertiesReader.getMciProperties(),
                propertiesReader.getShrProperties());

        ShrEncounter encounterComposition = new ShrEncounter(bundle, healthId, shrEncounterId);
        org.openmrs.Encounter newEmrEncounter = fhirMapper.map(emrPatient, encounterComposition, systemProperties);
        visitService.saveVisit(newEmrEncounter.getVisit());
        saveOrders(newEmrEncounter);
        Date publishedDate = DateUtil.parseDate(encounterBundle.getPublishedDate());
        addEncounterToIdMapping(newEmrEncounter, shrEncounterId, healthId, systemProperties, publishedDate);
        systemUserService.setOpenmrsShrSystemUserAsCreator(newEmrEncounter);
        systemUserService.setOpenmrsShrSystemUserAsCreator(newEmrEncounter.getVisit());
        savePatientDeathInfo(emrPatient);
    }

    private void addEncounterToIdMapping(Encounter newEmrEncounter, String externalUuid, String healthId, SystemProperties systemProperties, Date publishedDate) {
        String internalUuid = newEmrEncounter.getUuid();
        String shrEncounterUrl = getEncounterUrl(externalUuid, healthId, systemProperties);
        IdMapping idMapping = new IdMapping(internalUuid, externalUuid, ID_MAPPING_ENCOUNTER_TYPE, shrEncounterUrl, publishedDate);
        idMappingsRepository.saveOrUpdateMapping(idMapping);
    }

    private void saveOrders(Encounter newEmrEncounter) {
        List<Order> ordersList = new ArrayList<>(newEmrEncounter.getOrders());
        Collections.sort(ordersList, new Comparator<Order>() {
            @Override
            public int compare(Order o1, Order o2) {
                return o1.getDateActivated().compareTo(o2.getDateActivated());
            }
        });
        for (Order order : ordersList) {
            orderService.saveOrder(order, null);
        }
    }

    private boolean shouldSyncEncounter(String encounterId, EncounterBundle encounterBundle) {
        if (hasUpdatedEncounterInTheFeed(encounterBundle)) return false;
        IdMapping idMapping = idMappingsRepository.findByExternalId(encounterId);
        Date publishedDate = DateUtil.parseDate(encounterBundle.getPublishedDate());
        if (idMapping != null && publishedDate.before(idMapping.getLastSyncDateTime())) return false;
        return getEncounterConfidentiality(encounterBundle.getBundle()).ordinal() <= Confidentiality.Normal.ordinal();
    }

    private boolean hasUpdatedEncounterInTheFeed(EncounterBundle encounterBundle) {
        if (encounterBundle.getCategories() != null) {
            for (Object category : encounterBundle.getCategories()) {
                if (((Category) category).getTerm().contains(LATEST_UPDATE_CATEGORY_TAG)) return true;
            }
        }
        return false;
    }

    private Confidentiality getEncounterConfidentiality(Bundle bundle) {
        Composition composition = bundle.getAllPopulatedChildElementsOfType(Composition.class).get(0);
        String confidentialityCode = composition.getConfidentiality();
        if (null == confidentialityCode) {
            return Confidentiality.Normal;
        }
        return getConfidentiality(confidentialityCode);
    }

    private void savePatientDeathInfo(org.openmrs.Patient emrPatient) {
        if (emrPatient.isDead()) {
            emrPatient.setCauseOfDeath(patientDeathService.getCauseOfDeath(emrPatient));
            patientService.savePatient(emrPatient);
            systemUserService.setOpenmrsShrSystemUserAsCreator(emrPatient);
        }
    }
}
