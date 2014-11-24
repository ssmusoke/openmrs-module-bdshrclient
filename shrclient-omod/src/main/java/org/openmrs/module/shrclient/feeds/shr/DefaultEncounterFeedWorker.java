package org.openmrs.module.shrclient.feeds.shr;

import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.AtomFeed;
import org.ict4h.atomfeed.client.exceptions.AtomFeedClientException;
import org.openmrs.api.PatientService;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.handlers.ServiceClientRegistry;
import org.openmrs.module.shrclient.mci.api.model.Patient;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;

public class DefaultEncounterFeedWorker implements EncounterEventWorker {


    private IdMappingsRepository idMappingsRepository;
    private MciPatientService mciPatientService;
    private PropertiesReader propertiesReader;
    private PatientService patientService;


    private final Logger logger = Logger.getLogger(DefaultEncounterFeedWorker.class);

    public DefaultEncounterFeedWorker(MciPatientService mciPatientService, PatientService patientService,
                                      IdMappingsRepository idMappingsRepository, PropertiesReader propertiesReader) {
        this.idMappingsRepository = idMappingsRepository;
        this.mciPatientService = mciPatientService;
        this.propertiesReader = propertiesReader;
        this.patientService = patientService;
    }

    @Override
    public void process(EncounterBundle encounterBundle) {
        logger.info("Processing bundle with encounter id: " + encounterBundle.getEncounterId());
        AtomFeed feed = encounterBundle.getResourceOrFeed().getFeed();
        String healthId = identifyPatientHealthId(feed);
        org.openmrs.Patient emrPatient = identifyEmrPatient(healthId);
        try {
            if (emrPatient == null) {
                RestClient mciClient = new ServiceClientRegistry(propertiesReader).getMCIClient();
                Patient patient = mciClient.get(Constants.MCI_PATIENT_URL + "/" + healthId, Patient.class);
                emrPatient = mciPatientService.createOrUpdatePatient(patient);
                if (emrPatient == null) {
                    String message = String.format("Can not identify patient[%s]", healthId);
                    logger.error(message);
                    throw new Exception(message);
                }
                mciPatientService.updateEncounter(emrPatient, encounterBundle, healthId);
            }
        } catch (Exception e) {
            String message = String.format("Error occurred while trying to process encounter[%s] of patient[%s]",
                    encounterBundle.getEncounterId(), encounterBundle.getHealthId());
            logger.error(message);
            throw new AtomFeedClientException(message, e);
        }
    }

    private String identifyPatientHealthId(AtomFeed feed) {
        final org.hl7.fhir.instance.model.Encounter shrEncounter = FHIRFeedHelper.getEncounter(feed);
        return shrEncounter.getSubject().getReferenceSimple();
    }

    private org.openmrs.Patient identifyEmrPatient(String healthId) {
        IdMapping idMap = idMappingsRepository.findByExternalId(healthId);
        if (idMap != null) {
            logger.info("Patient with HealthId " + healthId + " already exists. Using reference to the patient for downloaded encounters.");
            return patientService.getPatientByUuid(idMap.getInternalId());
        }
        return null;
    }
}
