package org.openmrs.module.shrclient.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.*;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.mapper.PersonAttributeMapper;
import org.openmrs.module.shrclient.mapper.PhoneNumberMapper;
import org.openmrs.module.shrclient.mapper.RelationshipMapper;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.model.Status;
import org.openmrs.module.shrclient.service.BbsCodeService;
import org.openmrs.module.shrclient.service.HIEPatientDeathService;
import org.openmrs.module.shrclient.service.HIEPatientService;
import org.openmrs.module.shrclient.util.AddressHelper;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.openmrs.module.shrclient.util.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.openmrs.module.fhir.Constants.*;

@Service
public class HIEPatientServiceImpl implements HIEPatientService {
    private static final String DOB_TYPE_DECLARED = "1";
    private static final String DOB_TYPE_ESTIMATED = "3";

    private static final Logger logger = Logger.getLogger(HIEPatientServiceImpl.class);
    public static final String REGEX_TO_MATCH_MULTIPLE_WHITE_SPACE = "\\s+";

    private BbsCodeService bbsCodeService;
    private PatientService patientService;
    private IdMappingsRepository idMappingsRepository;
    private PropertiesReader propertiesReader;
    private SystemUserService systemUserService;
    private PersonAttributeMapper personAttributeMapper;
    private HIEPatientDeathService patientDeathService;

    @Autowired
    public HIEPatientServiceImpl(BbsCodeService bbsCodeService,
                                 PatientService patientService,
                                 PersonService personService,
                                 IdMappingsRepository idMappingsRepository,
                                 PropertiesReader propertiesReader,
                                 SystemUserService systemUserService, HIEPatientDeathService patientDeathService) {
        this.bbsCodeService = bbsCodeService;
        this.patientService = patientService;
        this.idMappingsRepository = idMappingsRepository;
        this.propertiesReader = propertiesReader;
        this.systemUserService = systemUserService;
        this.patientDeathService = patientDeathService;
        this.personAttributeMapper = new PersonAttributeMapper(personService);
    }

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

        addPersonAttribute(emrPatient, NATIONAL_ID_ATTRIBUTE, mciPatient.getNationalId());
        addPersonAttribute(emrPatient, HEALTH_ID_ATTRIBUTE, mciPatient.getHealthId());
        addPersonAttribute(emrPatient, BIRTH_REG_NO_ATTRIBUTE, mciPatient.getBirthRegNumber());
        addPersonAttribute(emrPatient, HOUSE_HOLD_CODE_ATTRIBUTE, mciPatient.getHouseHoldCode());
        String banglaName = mciPatient.getBanglaName();
        if (StringUtils.isNotBlank(banglaName)) {
            banglaName = banglaName.replaceAll(REGEX_TO_MATCH_MULTIPLE_WHITE_SPACE, " ");
        }
        addPersonAttribute(emrPatient, GIVEN_NAME_LOCAL, getGivenNameLocal(banglaName));
        addPersonAttribute(emrPatient, FAMILY_NAME_LOCAL, getFamilyNameLocal(banglaName));
        addPersonAttribute(emrPatient, PHONE_NUMBER, PhoneNumberMapper.map(mciPatient.getPhoneNumber()));
        mapRelations(emrPatient, mciPatient);

        String occupationConceptName = bbsCodeService.getOccupationConceptName(mciPatient.getOccupation());
        String occupationConceptId = getConceptId(occupationConceptName);
        if (occupationConceptId != null) {
            addPersonAttribute(emrPatient, OCCUPATION_ATTRIBUTE, occupationConceptId);
        } else {
            logger.warn(String.format("Can't update occupation for patient. " +
                            "Can't identify relevant concept for patient hid:%s, occupation:%s, code:%s",
                    mciPatient.getHealthId(), occupationConceptName, mciPatient.getOccupation()));
        }


        String educationConceptName = bbsCodeService.getEducationConceptName(mciPatient.getEducationLevel());
        String educationConceptId = getConceptId(educationConceptName);
        if (educationConceptId != null) {
            addPersonAttribute(emrPatient, EDUCATION_ATTRIBUTE, educationConceptId);
        } else {
            logger.warn(String.format("Can't update education for patient. " +
                            "Can't identify relevant concept for patient hid:%s, education:%s, code:%s",
                    mciPatient.getHealthId(), educationConceptName, mciPatient.getEducationLevel()));
        }

        Date dob = mciPatient.getDateOfBirth();
        emrPatient.setBirthdate(dob);
        if (DOB_TYPE_ESTIMATED.equals(mciPatient.getDobType())) {
            emrPatient.setBirthdateEstimated(Boolean.TRUE);
            emrPatient.setBirthdateEstimated(Boolean.FALSE);
        }

        org.openmrs.Patient patient = patientService.savePatient(emrPatient);
        systemUserService.setOpenmrsShrSystemUserAsCreator(emrPatient);
        addPatientToIdMapping(patient, mciPatient.getHealthId());
        return emrPatient;
    }

    private void mapRelations(org.openmrs.Patient emrPatient, Patient mciPatient) {
        new RelationshipMapper().addRelationAttributes(mciPatient.getRelations(), emrPatient, idMappingsRepository);
    }

    private PatientIdentifier generateIdentifier() {
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

    private String getFamilyNameLocal(String banglaName) {
        if (StringUtils.isBlank(banglaName)) {
            return null;
        }
        int lastIndexOfSpace = banglaName.lastIndexOf(" ");
        return (-1 != lastIndexOfSpace ? banglaName.substring(lastIndexOfSpace + 1) : "");
    }

    private String getGivenNameLocal(String banglaName) {
        if (StringUtils.isBlank(banglaName)) {
            return null;
        }
        int lastIndexOfSpace = banglaName.lastIndexOf(" ");
        return (-1 != lastIndexOfSpace ? banglaName.substring(0, lastIndexOfSpace) : banglaName);
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
            emrPatient.setDeathDate(status.getDateOfDeath());
            emrPatient.setCauseOfDeath(patientDeathService.getCauseOfDeath(emrPatient));
        }
    }

    private org.openmrs.Patient identifyEmrPatient(String healthId) {
        IdMapping idMap = idMappingsRepository.findByExternalId(healthId);
        if (idMap == null) return null;
        logger.info("Patient with HealthId " + healthId + " already exists. Using reference to the patient for downloaded encounters.");
        return patientService.getPatientByUuid(idMap.getInternalId());
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

    private void addPersonAttribute(org.openmrs.Patient emrPatient, String attributeName, String attributeValue) {
        PersonAttribute attribute = personAttributeMapper.getAttribute(attributeName, attributeValue);
        if (attribute != null) {
            emrPatient.addAttribute(attribute);
        } else {
            System.out.println("Attribute not defined: " + attributeName);
        }
    }

    private PatientIdentifierType getPatientIdentifierType() {
        AdministrationService administrationService = Context.getAdministrationService();
        String globalProperty = administrationService.getGlobalProperty(MRSProperties.GLOBAL_PROPERTY_EMR_PRIMARY_IDENTIFIER_TYPE);
        PatientIdentifierType patientIdentifierByUuid = Context.getPatientService().getPatientIdentifierTypeByUuid(globalProperty);
        return patientIdentifierByUuid;
    }

    private void addPatientToIdMapping(org.openmrs.Patient emrPatient, String healthId) {
        String patientUuid = emrPatient.getUuid();
        SystemProperties systemProperties = new SystemProperties(
                propertiesReader.getFrProperties(),
                propertiesReader.getTrProperties(),
                propertiesReader.getPrProperties(),
                propertiesReader.getFacilityInstanceProperties(),
                propertiesReader.getMciProperties(), new Properties());
        String url = new EntityReference().build(org.openmrs.Patient.class, systemProperties, healthId);
        idMappingsRepository.saveOrUpdateMapping(new IdMapping(patientUuid, healthId, ID_MAPPING_PATIENT_TYPE, url, new Date()));
    }
}
