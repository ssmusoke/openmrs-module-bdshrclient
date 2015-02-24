package org.openmrs.module.shrclient.service.impl;

import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.AtomFeed;
import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fhir.mapper.emr.FHIRMapper;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.service.BbsCodeService;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.util.AddressHelper;
import org.openmrs.module.shrclient.util.ParticipantHelper;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class MciPatientServiceImpl extends BaseOpenmrsService implements MciPatientService {

    public static final String CAUSE_OF_DEATH_NOT_SPECIFIED = "Not Specified";

    @Autowired
    private BbsCodeService bbsCodeService;

    @Autowired
    private EncounterService encounterService;

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
    private ProviderLookupService providerLookupService;

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

        addPersonAttribute(personService, emrPatient, Constants.NATIONAL_ID_ATTRIBUTE, mciPatient.getNationalId());
        addPersonAttribute(personService, emrPatient, Constants.HEALTH_ID_ATTRIBUTE, mciPatient.getHealthId());
        addPersonAttribute(personService, emrPatient, Constants.PRIMARY_CONTACT_ATTRIBUTE, mciPatient.getPrimaryContact());

        String occupationConceptName = bbsCodeService.getOccupationConceptName(mciPatient.getOccupation());
        String occupationConceptId = getConceptId(occupationConceptName);
        if (occupationConceptId != null) {
            addPersonAttribute(personService, emrPatient, Constants.OCCUPATION_ATTRIBUTE, occupationConceptId);
        } else {
            logger.warn(String.format("Can't update occupation for patient. " +
                            "Can't identify relevant concept for patient hid:%s, occupation:%s, code:%s",
                    mciPatient.getHealthId(), occupationConceptName, mciPatient.getOccupation()));
        }


        String educationConceptName = bbsCodeService.getEducationConceptName(mciPatient.getEducationLevel());
        String educationConceptId = getConceptId(educationConceptName);
        if (educationConceptId != null) {
            addPersonAttribute(personService, emrPatient, Constants.EDUCATION_ATTRIBUTE, educationConceptId);
        } else {
            logger.warn(String.format("Can't update education for patient. " +
                            "Can't identify relevant concept for patient hid:%s, education:%s, code:%s",
                    mciPatient.getHealthId(), educationConceptName, mciPatient.getEducationLevel()));
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constants.ISO_DATE_FORMAT);
        try {
            Date dob = simpleDateFormat.parse(mciPatient.getDateOfBirth());
            emrPatient.setBirthdate(dob);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        ParticipantHelper.setCreator(emrPatient,userService);
//        setCreator(emrPatient);
        org.openmrs.Patient patient = patientService.savePatient(emrPatient);
        addPatientToIdMapping(patient, mciPatient.getHealthId());
        return emrPatient;
    }

    private void setDeathInfo(org.openmrs.Patient emrPatient, Patient mciPatient) {
        Character status = mciPatient.getStatus();
        if (status == '1') {
            return;
        }
        if (status == '2') {
            emrPatient.setDead(true);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constants.ISO_DATE_FORMAT);
            String dateOfDeath = mciPatient.getDateOfDeath();
            if (dateOfDeath != null) {
                try {
                    Date dob = simpleDateFormat.parse(dateOfDeath);
                    emrPatient.setDeathDate(dob);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
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
            if (((SequentialIdentifierGenerator) identifierSource).getPrefix().equals(Constants.IDENTIFIER_SOURCE_NAME)) {
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

        setEncounterProviderAndCreator(newEmrEncounter);
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
        idMappingsRepository.saveMapping(new IdMapping(internalUuid, externalUuid, Constants.ID_MAPPING_ENCOUNTER_TYPE, url));
    }

    private void setEncounterProviderAndCreator(org.openmrs.Encounter newEmrEncounter) {
//        User systemUser = getOpenMRSDeamonUser();
        User systemUser = ParticipantHelper.getOpenMRSDeamonUser(userService);
        ParticipantHelper.setCreator(newEmrEncounter, systemUser);
        ParticipantHelper.setCreator(newEmrEncounter.getVisit(), systemUser);
//        setCreator(newEmrEncounter, systemUser);
//        setCreator(newEmrEncounter.getVisit(), systemUser);

        newEmrEncounter.addProvider(encounterService.getEncounterRoleByUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID), providerLookupService.getShrClientSystemProvider());
    }

    private boolean shouldSyncEncounter(String encounterId) {
        return (idMappingsRepository.findByExternalId(encounterId) == null) ? true : false;
    }


//    private void setCreator(Visit visit, User systemUser) {
//        if (visit.getCreator() == null) {
//            visit.setCreator(systemUser);
//        } else {
//            visit.setChangedBy(systemUser);
//        }
//    }

    private String getConceptId(String conceptName) {
        if (conceptName == null) {
            return null;
        }
        Concept concept = Context.getConceptService().getConceptByName(conceptName);
        return concept != null ? String.valueOf(concept.getConceptId()) : null;
    }

//    private void setCreator(org.openmrs.Patient emrPatient) {
//        User systemUser = getOpenMRSDeamonUser();
//        if (emrPatient.getCreator() == null) {
//            emrPatient.setCreator(systemUser);
//        } else {
//            emrPatient.setChangedBy(systemUser);
//        }
//
//    }

//    private void setCreator(org.openmrs.Encounter encounter, User systemUser) {
//        if (encounter.getCreator() == null) {
//            encounter.setCreator(systemUser);
//        } else {
//            encounter.setChangedBy(systemUser);
//        }
//    }

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

    private User getOpenMRSDeamonUser() {
        return userService.getUserByUuid(Constants.OPENMRS_DAEMON_USER);
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
        String globalProperty = administrationService.getGlobalProperty(Constants.EMR_PRIMARY_IDENTIFIER_TYPE);
        PatientIdentifierType patientIdentifierByUuid = Context.getPatientService().getPatientIdentifierTypeByUuid(globalProperty);
        return patientIdentifierByUuid;
    }

    private void addPatientToIdMapping(org.openmrs.Patient emrPatient, String healthId) {
        String patientUuid = emrPatient.getUuid();
        SystemProperties systemProperties = new SystemProperties(propertiesReader.getBaseUrls(), propertiesReader.getShrProperties(),
                propertiesReader.getFrProperties(), propertiesReader.getTrProperties(), propertiesReader.getPrProperties());
        String url = new EntityReference().build(org.openmrs.Patient.class, systemProperties, healthId);
        idMappingsRepository.saveMapping(new IdMapping(patientUuid, healthId, Constants.ID_MAPPING_PATIENT_TYPE, url));

    }
}
