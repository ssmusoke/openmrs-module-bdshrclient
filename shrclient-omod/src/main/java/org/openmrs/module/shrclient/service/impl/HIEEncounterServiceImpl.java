package org.openmrs.module.shrclient.service.impl;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import org.apache.log4j.Logger;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir.mapper.emr.FHIRMapper;
import org.openmrs.module.fhir.mapper.model.Confidentiality;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.service.HIEEncounterService;
import org.openmrs.module.shrclient.service.HIEPatientDeathService;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.openmrs.module.shrclient.util.SystemUserService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.openmrs.module.fhir.mapper.model.Confidentiality.getConfidentiality;

@Service
public class HIEEncounterServiceImpl implements HIEEncounterService {

    private static final Logger logger = Logger.getLogger(HIEEncounterServiceImpl.class);

    private PatientService patientService;
    private IdMappingsRepository idMappingsRepository;
    private PropertiesReader propertiesReader;
    private SystemUserService systemUserService;
    private VisitService visitService;
    private FHIRMapper fhirMapper;
    private OrderService orderService;
    private HIEPatientDeathService patientDeathService;

    @Autowired
    public HIEEncounterServiceImpl(PatientService patientService, IdMappingsRepository idMappingsRepository,
                                   PropertiesReader propertiesReader, SystemUserService systemUserService,
                                   VisitService visitService, FHIRMapper fhirMapper, OrderService orderService, HIEPatientDeathService patientDeathService) {
        this.patientService = patientService;
        this.idMappingsRepository = idMappingsRepository;
        this.propertiesReader = propertiesReader;
        this.systemUserService = systemUserService;
        this.visitService = visitService;
        this.fhirMapper = fhirMapper;
        this.orderService = orderService;
        this.patientDeathService = patientDeathService;
    }

    @Override
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

    @Override
    public void createOrUpdateEncounter(org.openmrs.Patient emrPatient, EncounterBundle encounterBundle, String healthId) throws Exception {
        String fhirEncounterId = encounterBundle.getEncounterId();
        Bundle bundle = encounterBundle.getBundle();
        logger.debug(String.format("Processing Encounter feed from SHR for patient[%s] with Encounter ID[%s]", encounterBundle.getHealthId(), fhirEncounterId));

        if (!shouldSyncEncounter(fhirEncounterId, bundle)) return;
        SystemProperties systemProperties = new SystemProperties(
                propertiesReader.getFrProperties(),
                propertiesReader.getTrProperties(),
                propertiesReader.getPrProperties(),
                propertiesReader.getFacilityInstanceProperties(),
                propertiesReader.getMciProperties(),
                propertiesReader.getShrProperties());
        org.openmrs.Encounter newEmrEncounter = fhirMapper.map(emrPatient, healthId, fhirEncounterId, bundle, systemProperties);
        visitService.saveVisit(newEmrEncounter.getVisit());
        saveOrders(newEmrEncounter);
        systemUserService.setOpenmrsShrSystemUserAsCreator(newEmrEncounter);
        systemUserService.setOpenmrsShrSystemUserAsCreator(newEmrEncounter.getVisit());
        savePatientDeathInfo(emrPatient);
    }

    private void savePatientDeathInfo(org.openmrs.Patient emrPatient) {
        if (emrPatient.isDead()) {
            emrPatient.setCauseOfDeath(patientDeathService.getCauseOfDeath(emrPatient));
            patientService.savePatient(emrPatient);
            systemUserService.setOpenmrsShrSystemUserAsCreator(emrPatient);
        }
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

    private boolean shouldSyncEncounter(String encounterId, Bundle bundle) {
        if (idMappingsRepository.findByExternalId(encounterId) != null) {
            return false;
        }
        if (getEncounterConfidentiality(bundle).ordinal() > Confidentiality.Normal.ordinal()) {
            return false;
        }
        return true;
    }

    private Confidentiality getEncounterConfidentiality(Bundle bundle) {
        Composition composition = bundle.getAllPopulatedChildElementsOfType(Composition.class).get(0);
        String confidentialityCode = composition.getConfidentiality();
        if (null == confidentialityCode) {
            return Confidentiality.Normal;
        }
        ;
        return getConfidentiality(confidentialityCode);
    }


}
