package org.openmrs.module.shrclient.service;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import com.sun.syndication.feed.atom.Category;
import org.apache.commons.lang3.StringUtils;
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
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.model.EncounterIdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.openmrs.module.shrclient.util.SystemUserService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.openmrs.module.fhir.mapper.model.Confidentiality.getConfidentiality;
import static org.openmrs.module.fhir.utils.SHREncounterURLUtil.getEncounterUrl;

@Service("hieEmrEncounterService")
public class EMREncounterService {

    private static final Logger logger = Logger.getLogger(EMREncounterService.class);

    private PatientService patientService;
    private IdMappingRepository idMappingRepository;
    private PropertiesReader propertiesReader;
    private SystemUserService systemUserService;
    private VisitService visitService;
    private FHIRMapper fhirMapper;
    private OrderService orderService;
    private EMRPatientDeathService patientDeathService;

    @Autowired
    public EMREncounterService(PatientService patientService, IdMappingRepository idMappingRepository,
                               PropertiesReader propertiesReader, SystemUserService systemUserService,
                               VisitService visitService, FHIRMapper fhirMapper, OrderService orderService, EMRPatientDeathService patientDeathService) {
        this.patientService = patientService;
        this.idMappingRepository = idMappingRepository;
        this.propertiesReader = propertiesReader;
        this.systemUserService = systemUserService;
        this.visitService = visitService;
        this.fhirMapper = fhirMapper;
        this.orderService = orderService;
        this.patientDeathService = patientDeathService;
    }

    public void createOrUpdateEncounters(org.openmrs.Patient emrPatient, List<EncounterEvent> bundles, String healthId) {
        ArrayList<EncounterEvent> failedEncounters = new ArrayList<>();
        for (EncounterEvent bundle : bundles) {
            try {
                createOrUpdateEncounter(emrPatient, bundle, healthId);
            } catch (Exception e) {
                failedEncounters.add(bundle);
            }
        }
        for (EncounterEvent failedEncounterEvent : failedEncounters) {
            try {
                createOrUpdateEncounter(emrPatient, failedEncounterEvent, healthId);
            } catch (Exception e) {
                //TODO do proper handling, write to log API?
                logger.error("error Occurred while trying to process Encounter from SHR.", e);
                throw new RuntimeException(e);
            }
        }
    }

    public void createOrUpdateEncounter(org.openmrs.Patient emrPatient, EncounterEvent encounterEvent, String healthId) throws Exception {
        String shrEncounterId = encounterEvent.getEncounterId();
        Bundle bundle = encounterEvent.getBundle();
        logger.debug(String.format("Processing Encounter feed from SHR for patient[%s] with Encounter ID[%s]", encounterEvent.getHealthId(), shrEncounterId));

        if (!shouldSyncEncounter(shrEncounterId, encounterEvent)) return;
        SystemProperties systemProperties = new SystemProperties(
                propertiesReader.getFrProperties(),
                propertiesReader.getTrProperties(),
                propertiesReader.getPrProperties(),
                propertiesReader.getFacilityInstanceProperties(),
                propertiesReader.getMciProperties(),
                propertiesReader.getShrProperties());

        ShrEncounter shrEncounter = new ShrEncounter(bundle, healthId, shrEncounterId);
        org.openmrs.Encounter newEmrEncounter = fhirMapper.map(emrPatient, shrEncounter, systemProperties);
        visitService.saveVisit(newEmrEncounter.getVisit());
        saveOrders(newEmrEncounter);
        Date encounterUpdatedDate = getEncounterUpdatedDate(encounterEvent);
        addEncounterToIdMapping(newEmrEncounter, shrEncounterId, healthId, systemProperties, encounterUpdatedDate);
        systemUserService.setOpenmrsShrSystemUserAsCreator(newEmrEncounter);
        systemUserService.setOpenmrsShrSystemUserAsCreator(newEmrEncounter.getVisit());
        savePatientDeathInfo(emrPatient);
    }

    private void addEncounterToIdMapping(Encounter newEmrEncounter, String externalUuid, String healthId, SystemProperties systemProperties, Date encounterUpdatedDate) {
        String internalUuid = newEmrEncounter.getUuid();
        String shrEncounterUrl = getEncounterUrl(externalUuid, healthId, systemProperties);
        EncounterIdMapping encounterIdMapping = new EncounterIdMapping(internalUuid, externalUuid, shrEncounterUrl, new Date(), encounterUpdatedDate);
        idMappingRepository.saveOrUpdateIdMapping(encounterIdMapping);
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

    private boolean shouldSyncEncounter(String encounterId, EncounterEvent encounterEvent) {
        if (hasUpdatedEncounterInTheFeed(encounterEvent)) return false;
        EncounterIdMapping encounterIdMapping = (EncounterIdMapping) idMappingRepository.findByExternalId(encounterId, IdMappingType.ENCOUNTER);
        Date encounterUpdatedDate = getEncounterUpdatedDate(encounterEvent);
        if (isUpdateAlreadyProcessed(encounterIdMapping, encounterUpdatedDate)) return false;
        return getEncounterConfidentiality(encounterEvent.getBundle()).ordinal() <= Confidentiality.Normal.ordinal();
    }

    private boolean isUpdateAlreadyProcessed(EncounterIdMapping encounterIdMapping, Date encounterUpdatedDate) {
        if (encounterIdMapping == null) return false;
        Date serverUpdateDateTime = encounterIdMapping.getServerUpdateDateTime();
        if (serverUpdateDateTime == null) return true;
        return encounterUpdatedDate.before(serverUpdateDateTime) || encounterUpdatedDate.equals(serverUpdateDateTime);
    }

    private Date getEncounterUpdatedDate(EncounterEvent encounterEvent) {
        Category encounterUpdatedCategory = encounterEvent.getEncounterUpdatedCategory();
        String encounterUpdatedDate = StringUtils.substringAfter(encounterUpdatedCategory.getTerm(), ":");
        return DateUtil.parseDate(encounterUpdatedDate);
    }

    private boolean hasUpdatedEncounterInTheFeed(EncounterEvent encounterEvent) {
        return encounterEvent.getLatestUpdateEventCategory() != null;
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
