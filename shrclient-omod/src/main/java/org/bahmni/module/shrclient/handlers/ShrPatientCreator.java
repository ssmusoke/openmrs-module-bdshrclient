package org.bahmni.module.shrclient.handlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bahmni.module.shrclient.mapper.PatientMapper;
import org.bahmni.module.shrclient.model.Patient;
import org.bahmni.module.shrclient.util.Constants;
import org.bahmni.module.shrclient.util.RestClient;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.User;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.UserService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShrPatientCreator implements EventWorker {

    private static final Logger log = Logger.getLogger(ShrPatientCreator.class);

    private PatientService patientService;
    private UserService userService;
    private PersonService personService;
    private PatientMapper patientMapper;
    private RestClient restClient;

    public ShrPatientCreator(PatientService patientService, UserService userService, PersonService personService,
                             PatientMapper patientMapper, RestClient restClient) {
        this.patientService = patientService;
        this.userService = userService;
        this.personService = personService;
        this.patientMapper = patientMapper;
        this.restClient = restClient;
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

            if (!shouldSyncPatient(openMrsPatient)) {
                log.debug(String.format("OpenMRS patient [%s] was created from SHR. Ignoring Patient Sync.", openMrsPatient));
                return;
            }
            Patient patient = patientMapper.map(openMrsPatient);
            log.debug("Patient: [ " + patient + "]");

            String healthId = restClient.post("/patient", patient);
            updateOpenMrsPatientHealthId(openMrsPatient, healthId);

        } catch (Exception e) {
            log.error("Error while processing patient sync event.", e);
            throw new RuntimeException(e);
        }
    }

    void updateOpenMrsPatientHealthId(org.openmrs.Patient openMrsPatient, String healthId) {
        log.debug(String.format("Trying to update OpenMRS patient [%s] with health id [%s]", openMrsPatient, healthId));

        if (StringUtils.isBlank(healthId)) {
            log.debug("Health id is blank. Hence, not updated.");
            return;
        }

        PersonAttribute healthIdAttribute = openMrsPatient.getAttribute(Constants.HEALTH_ID_ATTRIBUTE);
        if (healthIdAttribute != null && healthId.equals(healthIdAttribute.getValue())) {
            log.debug("OpenMRS patient health id is same as the health id provided. Hence, not updated.");
            return;
        }

        if (healthIdAttribute == null) {
            healthIdAttribute = new PersonAttribute();
            PersonAttributeType healthAttrType = personService.getPersonAttributeTypeByName(Constants.HEALTH_ID_ATTRIBUTE);
            healthIdAttribute.setAttributeType(healthAttrType);
            healthIdAttribute.setValue(healthId);
            openMrsPatient.addAttribute(healthIdAttribute);

        } else {
            healthIdAttribute.setValue(healthId);
        }

        User shrClientSystemUser = userService.getUserByUsername(Constants.SHR_CLIENT_SYSTEM_NAME);
        log.debug("SHR client system user: " + shrClientSystemUser);
        openMrsPatient.setChangedBy(shrClientSystemUser);

        patientService.savePatient(openMrsPatient);
        log.debug(String.format("OpenMRS patient updated."));
    }

    boolean shouldSyncPatient(org.openmrs.Patient openMrsPatient) {
        User changedByUser = openMrsPatient.getChangedBy();
        if (changedByUser == null) {
            changedByUser = openMrsPatient.getCreator();
        }
        User shrClientSystemUser = userService.getUserByUsername(Constants.SHR_CLIENT_SYSTEM_NAME);
        return !shrClientSystemUser.getId().equals(changedByUser.getId());
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
