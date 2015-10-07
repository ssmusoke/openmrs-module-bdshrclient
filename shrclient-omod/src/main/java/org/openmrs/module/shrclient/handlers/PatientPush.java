package org.openmrs.module.shrclient.handlers;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.Provider;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.DateUtil;
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
import java.util.Collection;
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
    private ProviderService providerService;

    public PatientPush(PatientService patientService, SystemUserService systemUserService, PersonService personService,
                       PatientMapper patientMapper, PropertiesReader propertiesReader, ClientRegistry clientRegistry,
                       IdMappingsRepository idMappingsRepository, ProviderService providerService) throws IdentityUnauthorizedException {
        this.patientService = patientService;
        this.systemUserService = systemUserService;
        this.personService = personService;
        this.patientMapper = patientMapper;
        this.propertiesReader = propertiesReader;
        this.providerService = providerService;
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

            IdMapping idMapping = idMappingsRepository.findByInternalId(openMrsPatient.getUuid());
            if (!shouldUploadPatient(openMrsPatient, event.getUpdatedDate(), idMapping)) {
                return;
            }

            SystemProperties systemProperties = getSystemProperties();
            Patient patient = patientMapper.map(openMrsPatient, systemProperties);
            log.debug("Patient: [ " + patient + "]");

            if (idMapping == null) {
                setProvider(patient, openMrsPatient, systemProperties);
                MciPatientUpdateResponse response = newPatient(patient);
                updateOpenMrsPatientHealthId(openMrsPatient, response.getHealthId());
            } else {
                String healthId = idMapping.getExternalId();
                String url = StringUtil.ensureSuffix(propertiesReader.getMciPatientContext(), "/") + healthId;
                MciPatientUpdateResponse response = updatePatient(patient, url);
                updateOpenMrsPatientHealthId(openMrsPatient, healthId);
            }
            patientUuidsProcessed.add(openMrsPatient.getUuid());
        } catch (Exception e) {
            log.error("Error while processing patient sync event.", e);
            throw new RuntimeException(e);
        }
    }

    private void setProvider(Patient patient, org.openmrs.Patient openMrsPatient, SystemProperties systemProperties) {
        Person person = null;
        person = openMrsPatient.getChangedBy() != null ? openMrsPatient.getChangedBy().getPerson() : openMrsPatient.getCreator().getPerson();
        if (null == person) return;
        Collection<Provider> providers = providerService.getProvidersByPerson(person);
        if (CollectionUtils.isEmpty(providers)) return;

        for (Provider provider : providers) {
            String identifier = provider.getIdentifier();
            if (!StringUtils.isBlank(identifier) && isHIEProvider(identifier)) {
                String providerUrl = new EntityReference().build(Provider.class, systemProperties, identifier);
                patient.setProviderReference(providerUrl);
                return;
            }
        }
    }

    private boolean isHIEProvider(String identifier) {
        try {
            Integer.parseInt(identifier);
        } catch (Exception e) {
            log.warn("Provider is not an HIE provider.");
            return false;
        }
        return true;
    }

    private boolean shouldUploadPatient(org.openmrs.Patient openMrsPatient, Date eventDate, IdMapping idMapping) {
        if (openMrsPatient == null) {
            log.debug(String.format("Invalid event. Patient does not exist."));
            return false;
        }

        if(eventDate != null && idMapping != null) {
            if(DateUtil.isLaterThan(idMapping.getLastSyncDateTime(), eventDate)) {
                log.debug(String.format("Patient [%s] already uploaded to MCI.", openMrsPatient.getUuid()));
                return false;
            }
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
        log.debug(String.format("Trying to update OpenMRS patient [%s] with health id [%s]", openMrsPatient.getUuid(), healthId));

        if (StringUtils.isBlank(healthId)) {
            log.debug("Health id is blank. Hence, not updated.");
            return;
        }

        PersonAttribute healthIdAttribute = openMrsPatient.getAttribute(HEALTH_ID_ATTRIBUTE);
        if (healthIdAttribute != null && healthId.equals(healthIdAttribute.getValue())) {
            log.debug("OpenMRS patient health id is same as the health id provided. Hence, not updated.");
            saveOrUpdateIdMapping(openMrsPatient, healthId);
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
        patientService.savePatient(openMrsPatient);
        saveOrUpdateIdMapping(openMrsPatient, healthId);
        systemUserService.setOpenmrsShrSystemUserAsCreator(openMrsPatient);

        log.debug(String.format("OpenMRS patient updated."));
    }

    private void saveOrUpdateIdMapping(org.openmrs.Patient emrPatient, String healthId) {
        String patientUuid = emrPatient.getUuid();
        String url = new EntityReference().build(org.openmrs.Patient.class, getSystemProperties(), healthId);
        idMappingsRepository.saveOrUpdateMapping(new IdMapping(patientUuid, healthId, ID_MAPPING_PATIENT_TYPE, url, new Date()));
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
