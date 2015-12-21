package org.openmrs.module.shrclient.feeds.shr;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
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
import org.openmrs.module.shrclient.util.StringUtil;
import org.openmrs.module.shrclient.web.controller.dto.EncounterEvent;

import static org.openmrs.module.fhir.utils.FHIRBundleHelper.getComposition;

public class DefaultEncounterFeedWorker implements EncounterEventWorker {
    private EMRPatientService emrPatientService;
    private PropertiesReader propertiesReader;
    private ClientRegistry clientRegistry;
    private EMREncounterService emrEncounterService;

    private final Logger logger = Logger.getLogger(DefaultEncounterFeedWorker.class);

    public DefaultEncounterFeedWorker(EMRPatientService emrPatientService, EMREncounterService emrEncounterService, PropertiesReader propertiesReader,
                                      ClientRegistry clientRegistry) {
        this.emrPatientService = emrPatientService;
        this.propertiesReader = propertiesReader;
        this.clientRegistry = clientRegistry;
        this.emrEncounterService = emrEncounterService;
    }

    @Override
    public void process(EncounterEvent encounterEvent) {
        logger.info("Processing bundle with encounter id: " + encounterEvent.getEncounterId());
        Bundle bundle = encounterEvent.getBundle();
        String healthId = identifyPatientHealthId(bundle);
        try {
            RestClient mciClient = clientRegistry.getMCIClient();
            Patient patient = mciClient.get(StringUtil.ensureSuffix(propertiesReader.getMciPatientContext(), "/") + healthId, Patient.class);
            org.openmrs.Patient emrPatient = emrPatientService.createOrUpdatePatient(patient);

            if (null == emrPatient) {
                String message = String.format("Can not identify patient[%s]", healthId);
                logger.error(message);
                throw new Exception(message);
            }
            emrEncounterService.createOrUpdateEncounter(emrPatient, encounterEvent, healthId);
        } catch (Exception e) {
            String message = String.format("Error occurred while trying to process encounter[%s] of patient[%s]",
                    encounterEvent.getEncounterId(), healthId);
            logger.error(message);
            throw new AtomFeedClientException(message, e);
        }
    }

    private String identifyPatientHealthId(Bundle bundle) {
        final Composition composition = getComposition(bundle);
        return new EntityReference().parse(org.openmrs.Patient.class, composition.getSubject().getReference().getValue());
    }
}
