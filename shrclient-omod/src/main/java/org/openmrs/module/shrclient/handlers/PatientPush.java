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
import org.openmrs.module.fhir.utils.SystemUserService;
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

            if (event.getUpdatedDate() != null && openMrsPatient.getDateChanged() != null &&
                    openMrsPatient.getDateChanged().after(event.getUpdatedDate())) {
                log.debug("The patient has been updated after this event again");
                return;
            }

            if (systemUserService.isUpdatedByOpenMRSDaemonUser(openMrsPatient)) {
                log.debug(String.format("Patient [%s] was created by SHR. Ignoring Patient Sync.",
                        openMrsPatient));
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
            addPatientToIdMapping(openMrsPatient, healthId);

        } else {
            healthIdAttribute.setValue(healthId);
        }

        systemUserService.setCreator(openMrsPatient);

        patientService.savePatient(openMrsPatient);
        log.debug(String.format("OpenMRS patient updated."));
    }

    private void addPatientToIdMapping(org.openmrs.Patient emrPatient, String healthId) {
        String patientUuid = emrPatient.getUuid();
        String url = new EntityReference().build(org.openmrs.Patient.class, getSystemProperties(), healthId);
        idMappingsRepository.saveMapping(new IdMapping(patientUuid, healthId, ID_MAPPING_PATIENT_TYPE, url));

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
