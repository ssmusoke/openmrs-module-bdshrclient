package org.openmrs.module.shrclient.feeds.shr;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import org.apache.log4j.Logger;
import org.ict4h.atomfeed.client.exceptions.AtomFeedClientException;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.shrclient.handlers.ClientRegistry;
import org.openmrs.module.shrclient.identity.IdentityStore;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.service.EMREncounterService;
import org.openmrs.module.shrclient.service.EMRPatientService;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;

import static org.openmrs.module.fhir.utils.FHIRBundleHelper.getEncounter;

public class DefaultEncounterFeedWorker implements EncounterEventWorker {
    private EMRPatientService emrPatientService;
    private PropertiesReader propertiesReader;
    private IdentityStore identityStore;
    private EMREncounterService emrEncounterService;

    private final Logger logger = Logger.getLogger(DefaultEncounterFeedWorker.class);

    public DefaultEncounterFeedWorker(EMRPatientService emrPatientService, PropertiesReader propertiesReader,
                                      IdentityStore identityStore, EMREncounterService emrEncounterService) {
        this.emrPatientService = emrPatientService;
        this.propertiesReader = propertiesReader;
        this.identityStore = identityStore;
        this.emrEncounterService = emrEncounterService;
    }

    @Override
    public void process(EncounterBundle encounterBundle) {
        logger.info("Processing bundle with encounter id: " + encounterBundle.getEncounterId());
        Bundle bundle = encounterBundle.getBundle();
        String healthId = identifyPatientHealthId(bundle);
        try {
            RestClient mciClient = new ClientRegistry(propertiesReader, identityStore).getMCIClient();
            Patient patient = mciClient.get(propertiesReader.getMciPatientContext() + "/" + healthId, Patient.class);
            org.openmrs.Patient emrPatient = emrPatientService.createOrUpdatePatient(patient);

            if (null == emrPatient) {
                String message = String.format("Can not identify patient[%s]", healthId);
                logger.error(message);
                throw new Exception(message);
            }
            emrEncounterService.createOrUpdateEncounter(emrPatient, encounterBundle, healthId);
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
