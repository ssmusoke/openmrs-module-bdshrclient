package org.openmrs.module.shrclient.feeds.shr;

import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.AtomFeed;
import org.ict4h.atomfeed.client.exceptions.AtomFeedClientException;
import org.openmrs.Concept;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.shrclient.handlers.ClientRegistry;
import org.openmrs.module.shrclient.identity.IdentityStore;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;

import java.util.Map;

import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getEncounter;

public class DefaultEncounterFeedWorker implements EncounterEventWorker {
    private MciPatientService mciPatientService;
    private PropertiesReader propertiesReader;
    private IdentityStore identityStore;
    private OMRSConceptLookup omrsConceptLookup;

    private final Logger logger = Logger.getLogger(DefaultEncounterFeedWorker.class);

    public DefaultEncounterFeedWorker(MciPatientService mciPatientService, PropertiesReader propertiesReader,
                                      IdentityStore identityStore, OMRSConceptLookup omrsConceptLookup) {
        this.mciPatientService = mciPatientService;
        this.propertiesReader = propertiesReader;
        this.identityStore = identityStore;
        this.omrsConceptLookup = omrsConceptLookup;
    }

    @Override
    public void process(EncounterBundle encounterBundle) {
        logger.info("Processing bundle with encounter id: " + encounterBundle.getEncounterId());
        AtomFeed feed = encounterBundle.getResourceOrFeed().getFeed();
        String healthId = identifyPatientHealthId(feed);
        try {
            RestClient mciClient = new ClientRegistry(propertiesReader, identityStore).getMCIClient();
            Patient patient = mciClient.get(propertiesReader.getMciPatientContext() + "/" + healthId, Patient.class);
            Map<String, Concept> conceptCache = omrsConceptLookup.getCauseOfDeathConceptCache();
            org.openmrs.Patient emrPatient = mciPatientService.createOrUpdatePatient(patient, conceptCache);

            if (null == emrPatient) {
                String message = String.format("Can not identify patient[%s]", healthId);
                logger.error(message);
                throw new Exception(message);
            }
            mciPatientService.updateEncounter(emrPatient, encounterBundle, healthId, conceptCache);
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
