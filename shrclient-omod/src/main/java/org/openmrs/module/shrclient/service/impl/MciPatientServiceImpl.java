package org.openmrs.module.shrclient.service.impl;

import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.AtomFeed;
import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fhir.mapper.emr.FHIRMapper;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.fhir.utils.ParticipantHelper;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.model.Status;
import org.openmrs.module.shrclient.service.BbsCodeService;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.util.AddressHelper;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

import static org.openmrs.module.fhir.utils.Constants.*;

@Component
public class MciPatientServiceImpl extends BaseOpenmrsService implements MciPatientService {

    public static final String CAUSE_OF_DEATH_NOT_SPECIFIED = "Not Specified";

    @Autowired
    private BbsCodeService bbsCodeService;

    @Autowired
    private VisitService visitService;

    @Autowired
    private FHIRMapper fhirMapper;

    @Autowired
    PatientService patientService;

    @Autowired
    PersonService personService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    private PropertiesReader propertiesReader;

    @Autowired
    private UserService userService;

    @Autowired
    private ConceptService conceptService;

    private static final Logger logger = Logger.getLogger(MciPatientServiceImpl.class);

    @Override
    public org.openmrs.Patient createOrUpdatePatient(Patient mciPatient) {
        AddressHelper addressHelper = new AddressHelper();
        org.openmrs.Patient emrPatient = identifyEmrPatient(mciPatient.getHealthId());
        if (emrPatient == null) {
            emrPatient = new org.openmrs.Patient();
        }
        emrPatient.setGender(mciPatient.getGender());
        setIdentifier(emrPatient);
        setPersonName(emrPatient, mciPatient);
        setDeathInfo(emrPatient, mciPatient);
        emrPatient.addAddress(addressHelper.setPersonAddress(emrPatient.getPersonAddress(), mciPatient.getAddress()));

        addPersonAttribute(personService, emrPatient, NATIONAL_ID_ATTRIBUTE, mciPatient.getNationalId());
        addPersonAttribute(personService, emrPatient, HEALTH_ID_ATTRIBUTE, mciPatient.getHealthId());
        addPersonAttribute(personService, emrPatient, BIRTH_REG_NO_ATTRIBUTE, mciPatient.getBirthRegNumber());
        addPersonAttribute(personService, emrPatient, UNIQUE_ID_ATTRIBUTE, mciPatient.getUniqueId());
        addPersonAttribute(personService, emrPatient, PRIMARY_CONTACT_ATTRIBUTE, mciPatient.getPrimaryContact());
        addPersonAttribute(personService, emrPatient, HOUSE_HOLD_CODE_ATTRIBUTE, mciPatient.getHouseHoldCode());

        String occupationConceptName = bbsCodeService.getOccupationConceptName(mciPatient.getOccupation());
        String occupationConceptId = getConceptId(occupationConceptName);
        if (occupationConceptId != null) {
            addPersonAttribute(personService, emrPatient, OCCUPATION_ATTRIBUTE, occupationConceptId);
        } else {
            logger.warn(String.format("Can't update occupation for patient. " +
                            "Can't identify relevant concept for patient hid:%s, occupation:%s, code:%s",
                    mciPatient.getHealthId(), occupationConceptName, mciPatient.getOccupation()));
        }


        String educationConceptName = bbsCodeService.getEducationConceptName(mciPatient.getEducationLevel());
        String educationConceptId = getConceptId(educationConceptName);
        if (educationConceptId != null) {
            addPersonAttribute(personService, emrPatient, EDUCATION_ATTRIBUTE, educationConceptId);
        } else {
            logger.warn(String.format("Can't update education for patient. " +
                            "Can't identify relevant concept for patient hid:%s, education:%s, code:%s",
                    mciPatient.getHealthId(), educationConceptName, mciPatient.getEducationLevel()));
        }

        Date dob = DateUtil.parseDate(mciPatient.getDateOfBirth());
        emrPatient.setBirthdate(dob);

        ParticipantHelper.setCreator(emrPatient, userService);
        org.openmrs.Patient patient = patientService.savePatient(emrPatient);
        addPatientToIdMapping(patient, mciPatient.getHealthId());
        return emrPatient;
    }

    private void setDeathInfo(org.openmrs.Patient emrPatient, Patient mciPatient) {
        Status status = mciPatient.getStatus();
        boolean isAliveMciPatient = status.getType() == '1' ? true : false;
        boolean isAliveEmrPatient = !emrPatient.isDead();
        if (isAliveMciPatient && isAliveEmrPatient) {
            return;
        } else if (isAliveMciPatient) {
            emrPatient.setDead(false);
            emrPatient.setCauseOfDeath(null);
            emrPatient.setDeathDate(null);
        } else {
            emrPatient.setDead(true);
            String dateOfDeath = status.getDateOfDeath();
            if (dateOfDeath != null) {
                Date dob = DateUtil.parseDate(dateOfDeath);
                emrPatient.setDeathDate(dob);
            }
            Concept causeOfDeath = conceptService.getConcept(CAUSE_OF_DEATH_NOT_SPECIFIED);
            emrPatient.setCauseOfDeath(causeOfDeath);
        }
    }

    @Override
    public PatientIdentifier generateIdentifier() {
        IdentifierSourceService identifierSourceService = Context.getService(IdentifierSourceService.class);
        List<IdentifierSource> allIdentifierSources = identifierSourceService.getAllIdentifierSources(false);
        for (IdentifierSource identifierSource : allIdentifierSources) {
            if (((SequentialIdentifierGenerator) identifierSource).getPrefix().equals(IDENTIFIER_SOURCE_NAME)) {
                String identifier = identifierSourceService.generateIdentifier(identifierSource, "MCI Patient");
                PatientIdentifierType identifierType = getPatientIdentifierType();
                return new PatientIdentifier(identifier, identifierType, null);
            }
        }
        return null;
    }

    @Override
    public void createOrUpdateEncounters(org.openmrs.Patient emrPatient, List<EncounterBundle> bundles, String healthId) {
        for (EncounterBundle bundle : bundles) {
            try {
                updateEncounter(emrPatient, bundle, healthId);
            } catch (Exception e) {
                //TODO do proper handling, write to log API?
                logger.error("error Occurred while trying to process Encounter from SHR.", e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void updateEncounter(org.openmrs.Patient emrPatient, EncounterBundle encounterBundle, String healthId) throws Exception {
        String fhirEncounterId = encounterBundle.getEncounterId();
        AtomFeed feed = encounterBundle.getResourceOrFeed().getFeed();
        logger.debug(String.format("Processing Encounter feed from SHR for patient[%s] with Encounter ID[%s]", encounterBundle.getHealthId(), fhirEncounterId));

        if (!shouldSyncEncounter(fhirEncounterId)) return;
        org.openmrs.Encounter newEmrEncounter = fhirMapper.map(emrPatient, feed);
        visitService.saveVisit(newEmrEncounter.getVisit());
        saveOrders(newEmrEncounter);
        addEncounterToIdMapping(newEmrEncounter, fhirEncounterId, healthId);
    }

    private org.openmrs.Patient identifyEmrPatient(String healthId) {
        IdMapping idMap = idMappingsRepository.findByExternalId(healthId);
        if (idMap == null) return null;
        logger.info("Patient with HealthId " + healthId + " already exists. Using reference to the patient for downloaded encounters.");
        return patientService.getPatientByUuid(idMap.getInternalId());
    }

    private void saveOrders(Encounter newEmrEncounter) {
        for (Order order : newEmrEncounter.getOrders()) {
            orderService.saveOrder(order, null);
        }
    }

    private void addEncounterToIdMapping(Encounter newEmrEncounter, String externalUuid, String healthId) {
        String internalUuid = newEmrEncounter.getUuid();
        //TODO : put the right url
        String url = propertiesReader.getShrBaseUrl() +
                "/patients/" + healthId + "/encounters/" + externalUuid;
        idMappingsRepository.saveMapping(new IdMapping(internalUuid, externalUuid, ID_MAPPING_ENCOUNTER_TYPE, url));
    }

    private boolean shouldSyncEncounter(String encounterId) {
        return (idMappingsRepository.findByExternalId(encounterId) == null) ? true : false;
    }

    private String getConceptId(String conceptName) {
        if (conceptName == null) {
            return null;
        }
        Concept concept = Context.getConceptService().getConceptByName(conceptName);
        return concept != null ? String.valueOf(concept.getConceptId()) : null;
    }

    private void setIdentifier(org.openmrs.Patient emrPatient) {
        PatientIdentifier patientIdentifier = emrPatient.getPatientIdentifier();
        if (patientIdentifier == null) {
            patientIdentifier = generateIdentifier();
            patientIdentifier.setPreferred(true);
            emrPatient.addIdentifier(patientIdentifier);
        }
    }

    private void setPersonName(org.openmrs.Patient emrPatient, Patient mciPatient) {
        PersonName emrPersonName = emrPatient.getPersonName();
        if (emrPersonName == null) {
            emrPersonName = new PersonName();
            emrPersonName.setPreferred(true);
            emrPatient.addName(emrPersonName);
        }
        emrPersonName.setGivenName(mciPatient.getGivenName());
        emrPersonName.setFamilyName(mciPatient.getSurName());
    }

    private void addPersonAttribute(PersonService personService, org.openmrs.Patient emrPatient, String attributeName, String attributeValue) {
        PersonAttribute attribute = new PersonAttribute();
        PersonAttributeType attributeType = personService.getPersonAttributeTypeByName(attributeName);
        if (attributeType != null) {
            attribute.setAttributeType(attributeType);
            attribute.setValue(attributeValue);
            emrPatient.addAttribute(attribute);
        } else {
            System.out.println("Attribute not defined: " + attributeName);
        }
    }

    private PatientIdentifierType getPatientIdentifierType() {
        AdministrationService administrationService = Context.getAdministrationService();
        String globalProperty = administrationService.getGlobalProperty(EMR_PRIMARY_IDENTIFIER_TYPE);
        PatientIdentifierType patientIdentifierByUuid = Context.getPatientService().getPatientIdentifierTypeByUuid(globalProperty);
        return patientIdentifierByUuid;
    }

    private void addPatientToIdMapping(org.openmrs.Patient emrPatient, String healthId) {
        String patientUuid = emrPatient.getUuid();
        SystemProperties systemProperties = new SystemProperties(propertiesReader.getBaseUrls(),
                propertiesReader.getFrProperties(),
                propertiesReader.getTrProperties(),
                propertiesReader.getPrProperties(),
                propertiesReader.getFacilityInstanceProperties());
        String url = new EntityReference().build(org.openmrs.Patient.class, systemProperties, healthId);
        idMappingsRepository.saveMapping(new IdMapping(patientUuid, healthId, ID_MAPPING_PATIENT_TYPE, url));
    }
}
