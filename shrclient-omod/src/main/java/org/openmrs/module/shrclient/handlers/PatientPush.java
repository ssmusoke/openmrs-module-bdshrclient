package org.openmrs.module.shrclient.handlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.mapper.PatientMapper;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.model.mci.api.MciPatientUpdateResponse;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.StringUtil;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.openmrs.module.shrclient.util.SystemUserService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openmrs.module.fhir.utils.Constants.HEALTH_ID_ATTRIBUTE;
import static org.openmrs.module.fhir.utils.Constants.ID_MAPPING_PATIENT_TYPE;

public class PatientPush implements EventWorker {

    private static final Logger log = Logger.getLogger(PatientPush.class);
    private final ClientRegistry clientRegistry;

    private PatientService patientService;
    private SystemUserService systemUserService;
    private PersonService personService;
    private PatientMapper patientMapper;
    private PropertiesReader propertiesReader;
    private List<String> patientUuidsProcessed;
    private RestClient mciRestClient;
    private IdMappingsRepository idMappingsRepository;

    public PatientPush(PatientService patientService, SystemUserService systemUserService, PersonService personService,
                       PatientMapper patientMapper, PropertiesReader propertiesReader, ClientRegistry clientRegistry,
                       IdMappingsRepository idMappingsRepository) throws IdentityUnauthorizedException {
        this.patientService = patientService;
        this.systemUserService = systemUserService;
        this.personService = personService;
        this.patientMapper = patientMapper;
        this.propertiesReader = propertiesReader;
        this.mciRestClient = clientRegistry.getMCIClient();
        this.idMappingsRepository = idMappingsRepository;
        this.clientRegistry = clientRegistry;
        this.patientUuidsProcessed = new ArrayList<>();
    }

    @Override
    public void process(Event event) {
        log.debug("Event: [" + event + "]");
        try {
            String uuid = getPatientUuid(event);
            log.debug("Patient uuid: [" + uuid + "]");

            org.openmrs.Patient openMrsPatient = patientService.getPatientByUuid(uuid);

            if (!shouldUploadPatient(openMrsPatient)) {
                return;
            }

            Patient patient = patientMapper.map(openMrsPatient, getSystemProperties());
            log.debug("Patient: [ " + patient + "]");

            PersonAttribute healthIdAttribute = openMrsPatient.getAttribute(HEALTH_ID_ATTRIBUTE);

            if (healthIdAttribute == null) {
                MciPatientUpdateResponse response = newPatient(patient);
                updateOpenMrsPatientHealthId(openMrsPatient, response.getHealthId());
            } else {
                String healthId = healthIdAttribute.getValue();
                String url = StringUtil.ensureSuffix(propertiesReader.getMciPatientContext(), "/") + healthId;
                MciPatientUpdateResponse response = updatePatient(patient, url);
                saveIdMapping(openMrsPatient, healthId);
            }
            patientUuidsProcessed.add(openMrsPatient.getUuid());
        } catch (Exception e) {
            log.error("Error while processing patient sync event.", e);
            throw new RuntimeException(e);
        }
    }

    private boolean shouldUploadPatient(org.openmrs.Patient openMrsPatient) {
        if (openMrsPatient == null) {
            log.debug(String.format("No OpenMRS patient exists with uuid: [%s].", openMrsPatient.getUuid()));
            return false;
        }

        if (systemUserService.isUpdatedByOpenMRSShrSystemUser(openMrsPatient)) {
            log.debug(String.format("Patient [%s] was created by SHR. Ignoring Patient Sync.",
                    openMrsPatient));
            return false;
        }

        /*
        * To avoid processing of events for the same encounter in a job run, we store the encounter uuids processed in this job run.
        * It is cleared once the job has finished processing.
        */
        if (patientUuidsProcessed.contains(openMrsPatient.getUuid())) {
            log.debug(String.format("Patient[%s] has been processed in the same job run before.", openMrsPatient.getUuid()));
            return false;
        }
        return true;
    }

    private MciPatientUpdateResponse updatePatient(Patient patient, String url) throws IdentityUnauthorizedException {
        try {
            return mciRestClient.put(url, patient, MciPatientUpdateResponse.class);
        } catch (IdentityUnauthorizedException e) {
            log.error("Clearing unauthorized identity token.");
            clientRegistry.clearIdentityToken();
            throw e;
        }
    }

    private MciPatientUpdateResponse newPatient(Patient patient) throws IdentityUnauthorizedException {
        try {
            return mciRestClient.post(propertiesReader.getMciPatientContext(), patient,
                    MciPatientUpdateResponse.class);
        } catch (IdentityUnauthorizedException e) {
            log.error("Clearing unauthorized identity token.");
            clientRegistry.clearIdentityToken();
            throw e;
        }
    }

    void updateOpenMrsPatientHealthId(org.openmrs.Patient openMrsPatient, String healthId) {
        log.debug(String.format("Trying to update OpenMRS patient [%s] with health id [%s]", openMrsPatient, healthId));

        if (StringUtils.isBlank(healthId)) {
            log.debug("Health id is blank. Hence, not updated.");
            return;
        }

        PersonAttribute healthIdAttribute = openMrsPatient.getAttribute(HEALTH_ID_ATTRIBUTE);
        if (healthIdAttribute != null && healthId.equals(healthIdAttribute.getValue())) {
            log.debug("OpenMRS patient health id is same as the health id provided. Hence, not updated.");
            return;
        }

        if (healthIdAttribute == null) {
            healthIdAttribute = new PersonAttribute();
            PersonAttributeType healthAttrType = personService.getPersonAttributeTypeByName(HEALTH_ID_ATTRIBUTE);
            healthIdAttribute.setAttributeType(healthAttrType);
            healthIdAttribute.setValue(healthId);
            openMrsPatient.addAttribute(healthIdAttribute);

        } else {
            healthIdAttribute.setValue(healthId);
        }
        saveIdMapping(openMrsPatient, healthId);
        patientService.savePatient(openMrsPatient);
        systemUserService.setOpenmrsShrSystemUserAsCreator(openMrsPatient);

        log.debug(String.format("OpenMRS patient updated."));
    }

    private void saveIdMapping(org.openmrs.Patient emrPatient, String healthId) {
        String patientUuid = emrPatient.getUuid();
        String url = new EntityReference().build(org.openmrs.Patient.class, getSystemProperties(), healthId);
        Date dateUpdated = emrPatient.getDateChanged() != null ? emrPatient.getDateChanged() : emrPatient.getDateCreated();
        idMappingsRepository.saveMapping(new IdMapping(patientUuid, healthId, ID_MAPPING_PATIENT_TYPE, url, dateUpdated));
    }

    private SystemProperties getSystemProperties() {
        return new SystemProperties(propertiesReader.getBaseUrls(),
                propertiesReader.getFrProperties(),
                propertiesReader.getTrProperties(),
                propertiesReader.getPrProperties(),
                propertiesReader.getFacilityInstanceProperties(),
                propertiesReader.getMciProperties());
    }

    String getPatientUuid(Event event) {
        String patientUuid = null;
        Pattern p = Pattern.compile("^\\/openmrs\\/ws\\/rest\\/v1\\/patient\\/(.*)\\?v=full");
        Matcher m = p.matcher(event.getContent());
        if (m.matches()) {
            patientUuid = m.group(1);
        }
        return patientUuid;
    }

    @Override
    public void cleanUp(Event event) {
    }
}
