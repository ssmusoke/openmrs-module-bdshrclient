package org.openmrs.module.shrclient.feeds.shr;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import org.apache.log4j.Logger;
import org.ict4h.atomfeed.client.exceptions.AtomFeedClientException;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.shrclient.handlers.ClientRegistry;
import org.openmrs.module.shrclient.identity.IdentityStore;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.service.impl.HIEEncounterService;
import org.openmrs.module.shrclient.service.impl.HIEPatientService;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;

import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getEncounter;

public class DefaultEncounterFeedWorker implements EncounterEventWorker {
    private HIEPatientService hiePatientService;
    private PropertiesReader propertiesReader;
    private IdentityStore identityStore;
    private HIEEncounterService hieEncounterService;

    private final Logger logger = Logger.getLogger(DefaultEncounterFeedWorker.class);

    public DefaultEncounterFeedWorker(HIEPatientService hiePatientService, PropertiesReader propertiesReader,
                                      IdentityStore identityStore, HIEEncounterService hieEncounterService) {
        this.hiePatientService = hiePatientService;
        this.propertiesReader = propertiesReader;
        this.identityStore = identityStore;
        this.hieEncounterService = hieEncounterService;
    }

    @Override
    public void process(EncounterBundle encounterBundle) {
        logger.info("Processing bundle with encounter id: " + encounterBundle.getEncounterId());
        Bundle bundle = encounterBundle.getBundle();
        String healthId = identifyPatientHealthId(bundle);
        try {
            RestClient mciClient = new ClientRegistry(propertiesReader, identityStore).getMCIClient();
            Patient patient = mciClient.get(propertiesReader.getMciPatientContext() + "/" + healthId, Patient.class);
            org.openmrs.Patient emrPatient = hiePatientService.createOrUpdatePatient(patient);

            if (null == emrPatient) {
                String message = String.format("Can not identify patient[%s]", healthId);
                logger.error(message);
                throw new Exception(message);
            }
            hieEncounterService.createOrUpdateEncounter(emrPatient, encounterBundle, healthId);
        } catch (Exception e) {
            String message = String.format("Error occurred while trying to process encounter[%s] of patient[%s]",
                    encounterBundle.getEncounterId(), healthId);
            logger.error(message);
            throw new AtomFeedClientException(message, e);
        }
    }

    private String identifyPatientHealthId(Bundle bundle) {
        final Encounter shrEncounter = getEncounter(bundle);
        return new EntityReference().parse(org.openmrs.Patient.class, shrEncounter.getPatient().getReference().getValue());
    }
}
