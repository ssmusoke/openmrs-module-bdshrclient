package org.openmrs.module.shrclient.feeds.shr;

import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.AtomFeed;
import org.ict4h.atomfeed.client.exceptions.AtomFeedClientException;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;
import org.openmrs.module.shrclient.handlers.ClientRegistry;
import org.openmrs.module.shrclient.identity.IdentityStore;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.util.ConceptCache;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;

import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getEncounter;

public class DefaultEncounterFeedWorker implements EncounterEventWorker {
    private MciPatientService mciPatientService;
    private PropertiesReader propertiesReader;
    private IdentityStore identityStore;

    private final Logger logger = Logger.getLogger(DefaultEncounterFeedWorker.class);
    private ConceptCache conceptCache;

    public DefaultEncounterFeedWorker(MciPatientService mciPatientService, PropertiesReader propertiesReader,
                                      IdentityStore identityStore, ConceptService conceptService, GlobalPropertyLookUpService globalPropertyLookUpService) {
        this.mciPatientService = mciPatientService;
        this.propertiesReader = propertiesReader;
        this.identityStore = identityStore;
        conceptCache = new ConceptCache(conceptService, globalPropertyLookUpService);
    }

    @Override
    public void process(EncounterBundle encounterBundle) {
        logger.info("Processing bundle with encounter id: " + encounterBundle.getEncounterId());
        AtomFeed feed = encounterBundle.getFeed();
        String healthId = identifyPatientHealthId(feed);
        try {
            RestClient mciClient = new ClientRegistry(propertiesReader, identityStore).getMCIClient();
            Patient patient = mciClient.get(propertiesReader.getMciPatientContext() + "/" + healthId, Patient.class);
            org.openmrs.Patient emrPatient = mciPatientService.createOrUpdatePatient(patient, conceptCache);

            if (null == emrPatient) {
                String message = String.format("Can not identify patient[%s]", healthId);
                logger.error(message);
                throw new Exception(message);
            }
            mciPatientService.createOrUpdateEncounter(emrPatient, encounterBundle, healthId, conceptCache);
        } catch (Exception e) {
            String message = String.format("Error occurred while trying to process encounter[%s] of patient[%s]",
                    encounterBundle.getEncounterId(), encounterBundle.getHealthId());
            logger.error(message);
            throw new AtomFeedClientException(message, e);
        }
    }

    private String identifyPatientHealthId(AtomFeed feed) {
        final org.hl7.fhir.instance.model.Encounter shrEncounter = getEncounter(feed);
        return new EntityReference().parse(org.openmrs.Patient.class, shrEncounter.getSubject().getReferenceSimple());
    }
}
