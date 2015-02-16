package org.openmrs.module.shrclient.handlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.User;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.UserService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.mapper.PatientMapper;
import org.openmrs.module.shrclient.model.mci.api.MciPatientUpdateResponse;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openmrs.module.fhir.utils.Constants.*;

public class PatientPush implements EventWorker {

    private static final Logger log = Logger.getLogger(PatientPush.class);
    private final ClientRegistry clientRegistry;

    private PatientService patientService;
    private UserService userService;
    private PersonService personService;
    private PatientMapper patientMapper;
    private PropertiesReader propertiesReader;
    private RestClient mciRestClient;
    private IdMappingsRepository idMappingsRepository;

    public PatientPush(PatientService patientService, UserService userService, PersonService personService,
                       PatientMapper patientMapper, PropertiesReader propertiesReader, ClientRegistry clientRegistry,
                       IdMappingsRepository idMappingsRepository) throws IdentityUnauthorizedException {
        this.patientService = patientService;
        this.userService = userService;
        this.personService = personService;
        this.patientMapper = patientMapper;
        this.propertiesReader = propertiesReader;
        this.mciRestClient = clientRegistry.getMCIClient();
        this.idMappingsRepository = idMappingsRepository;
        this.clientRegistry = clientRegistry;
    }

    @Override
    public void process(Event event) {
        log.debug("Event: [" + event + "]");
        try {
            String uuid = getPatientUuid(event);
            log.debug("Patient uuid: [" + uuid + "]");

            org.openmrs.Patient openMrsPatient = patientService.getPatientByUuid(uuid);
            if (openMrsPatient == null) {
                log.debug(String.format("No OpenMRS patient exists with uuid: [%s].", uuid));
                return;
            }

            if (!isUpdatedByEmrUser(openMrsPatient)) {
                log.debug(String.format("OpenMRS patient [%s] was created from SHR. Ignoring Patient Sync.",
                        openMrsPatient));
                return;
            }

            Patient patient = patientMapper.map(openMrsPatient);
            log.debug("Patient: [ " + patient + "]");

            PersonAttribute healthIdAttribute = openMrsPatient.getAttribute(HEALTH_ID_ATTRIBUTE);


            if (healthIdAttribute == null) {
                MciPatientUpdateResponse response = newPatient(patient);
                updateOpenMrsPatientHealthId(openMrsPatient, response.getHealthId());
            } else {
                String healthId = healthIdAttribute.getValue();
                String url = MCI_PATIENT_URL + "/" + healthId;
                MciPatientUpdateResponse response = updatePatient(patient, url);
            }
        } catch (Exception e) {
            log.error("Error while processing patient sync event.", e);
            throw new RuntimeException(e);
        }
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
            return mciRestClient.post(MCI_PATIENT_URL, patient,
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
            addPatientToIdMapping(openMrsPatient, healthId);

        } else {
            healthIdAttribute.setValue(healthId);
        }

        User shrClientSystemUser = getShrClientSystemUser();
        log.debug("SHR client system user: " + shrClientSystemUser);
        openMrsPatient.setChangedBy(shrClientSystemUser);

        patientService.savePatient(openMrsPatient);
        log.debug(String.format("OpenMRS patient updated."));
    }

    boolean isUpdatedByEmrUser(org.openmrs.Patient openMrsPatient) {
        User changedByUser = openMrsPatient.getChangedBy();
        if (changedByUser == null) {
            changedByUser = openMrsPatient.getCreator();
        }
        User shrClientSystemUser = getShrClientSystemUser();
        return !shrClientSystemUser.getId().equals(changedByUser.getId());
    }

    private void addPatientToIdMapping(org.openmrs.Patient emrPatient, String healthId) {
        String patientUuid = emrPatient.getUuid();
        SystemProperties systemProperties = new SystemProperties(propertiesReader.getBaseUrls(), propertiesReader
                .getShrProperties(), propertiesReader.getFrProperties(), propertiesReader.getTrProperties());
        String url = new EntityReference().build(org.openmrs.Patient.class, systemProperties, healthId);
        idMappingsRepository.saveMapping(new IdMapping(patientUuid, healthId, ID_MAPPING_PATIENT_TYPE, url));

    }

    private User getShrClientSystemUser() {
        //OpenMRS Daemon user. UUID hardcoded in org.openmrs.api.Context.Daemon
        return userService.getUserByUuid(OPENMRS_DAEMON_USER);
        //return userService.getUserByUsername(Constants.SHR_CLIENT_SYSTEM_NAME);
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
