package org.openmrs.module.shrclient.service.impl;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.emr.FHIRMapper;
import org.openmrs.module.fhir.mapper.model.Confidentiality;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;
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
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.util.AddressHelper;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.openmrs.module.shrclient.util.SystemUserService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.openmrs.module.fhir.Constants.*;
import static org.openmrs.module.fhir.MRSProperties.*;
import static org.openmrs.module.fhir.mapper.model.Confidentiality.getConfidentiality;

@Component
public class MciPatientServiceImpl extends BaseOpenmrsService implements MciPatientService {
    private static final String DOB_TYPE_DECLARED = "1";
    private static final String DOB_TYPE_ESTIMATED = "3";

    private static final Logger logger = Logger.getLogger(MciPatientServiceImpl.class);
    public static final String REGEX_TO_MATCH_MULTIPLE_WHITE_SPACE = "\\s+";

    private BbsCodeService bbsCodeService;
    private VisitService visitService;
    private FHIRMapper fhirMapper;
    private PatientService patientService;
    private PersonService personService;
    private OrderService orderService;
    private IdMappingsRepository idMappingsRepository;
    private PropertiesReader propertiesReader;
    private SystemUserService systemUserService;
    private ObsService obsService;
    private ConceptService conceptService;
    private GlobalPropertyLookUpService globalPropertyLookUpService;
    private PersonAttributeMapper personAttributeMapper;

    @Autowired
    public MciPatientServiceImpl(BbsCodeService bbsCodeService,
                                 VisitService visitService,
                                 FHIRMapper fhirMapper,
                                 PatientService patientService,
                                 PersonService personService,
                                 OrderService orderService,
                                 IdMappingsRepository idMappingsRepository,
                                 PropertiesReader propertiesReader,
                                 SystemUserService systemUserService,
                                 ObsService obsService,
                                 ConceptService conceptService,
                                 GlobalPropertyLookUpService globalPropertyLookUpService) {
        this.bbsCodeService = bbsCodeService;
        this.visitService = visitService;
        this.fhirMapper = fhirMapper;
        this.patientService = patientService;
        this.personService = personService;
        this.orderService = orderService;
        this.idMappingsRepository = idMappingsRepository;
        this.propertiesReader = propertiesReader;
        this.systemUserService = systemUserService;
        this.obsService = obsService;
        this.conceptService = conceptService;
        this.globalPropertyLookUpService = globalPropertyLookUpService;
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
        } else {
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

    @Override
    public Concept getCauseOfDeath(org.openmrs.Patient emrPatient) {
        String causeOfDeathConceptId = globalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH);
        Concept unspecifiedCauseOfDeathConcept = causeOfDeathConceptId != null ? conceptService.getConcept(Integer.parseInt(causeOfDeathConceptId)) : conceptService.getConceptByName(TR_CONCEPT_CAUSE_OF_DEATH);
        String unspecifiedCauseOfDeathConceptId = globalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH);
        Concept causeOfDeathConcept = unspecifiedCauseOfDeathConceptId != null ? conceptService.getConcept(Integer.parseInt(unspecifiedCauseOfDeathConceptId)) : conceptService.getConceptByName(TR_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH);
        String error = null;
        if (emrPatient.isDead() && unspecifiedCauseOfDeathConcept == null) {
            error = String.format("Global Property %s is not set & Concept with name %s is not found", GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH, TR_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH);
            logger.error(error);
            throw new RuntimeException(error);
        }
        if (emrPatient.isDead() && causeOfDeathConcept == null) {
            error = String.format("Global Property %s is not set & Concept with name %s is not found", GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, TR_CONCEPT_CAUSE_OF_DEATH);
            logger.error(error);
            throw new RuntimeException(error);
        }
        if (emrPatient.isDead() && emrPatient.getCauseOfDeath() != null && emrPatient.getCauseOfDeath() != unspecifiedCauseOfDeathConcept) {
            return emrPatient.getCauseOfDeath();
        }
        Concept causeOfDeath = unspecifiedCauseOfDeathConcept;
        if (emrPatient.getId() != null) {
            List<Obs> obsForCauseOfDeath = obsService.getObservationsByPersonAndConcept(emrPatient, causeOfDeathConcept);
            if ((obsForCauseOfDeath != null) && !obsForCauseOfDeath.isEmpty()) {
                for (Obs obs : obsForCauseOfDeath) {
                    if (!obs.isVoided()) {
                        causeOfDeath = obs.getValueCoded();
                    }
                }
            }
        }
        return causeOfDeath;
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
                createOrUpdateEncounter(emrPatient, bundle, healthId);
            } catch (Exception e) {
                //TODO do proper handling, write to log API?
                logger.error("error Occurred while trying to process Encounter from SHR.", e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void createOrUpdateEncounter(org.openmrs.Patient emrPatient, EncounterBundle encounterBundle, String healthId) throws Exception {
        String fhirEncounterId = StringUtils.substringAfter(encounterBundle.getTitle(), "Encounter:");
        Bundle bundle = encounterBundle.getBundle();
        logger.debug(String.format("Processing Encounter feed from SHR for patient[%s] with Encounter ID[%s]", encounterBundle.getHealthId(), fhirEncounterId));

        if (!shouldSyncEncounter(fhirEncounterId, bundle)) return;
        SystemProperties systemProperties = new SystemProperties(
                propertiesReader.getFrProperties(),
                propertiesReader.getTrProperties(),
                propertiesReader.getPrProperties(),
                propertiesReader.getFacilityInstanceProperties(),
                propertiesReader.getMciProperties(),
                propertiesReader.getShrProperties());
        org.openmrs.Encounter newEmrEncounter = fhirMapper.map(emrPatient, healthId, fhirEncounterId, bundle, systemProperties);
        visitService.saveVisit(newEmrEncounter.getVisit());
        saveOrders(newEmrEncounter);
        systemUserService.setOpenmrsShrSystemUserAsCreator(newEmrEncounter);
        systemUserService.setOpenmrsShrSystemUserAsCreator(newEmrEncounter.getVisit());
        savePatientDeathInfo(emrPatient);
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
            emrPatient.setCauseOfDeath(getCauseOfDeath(emrPatient));
        }
    }

    private void savePatientDeathInfo(org.openmrs.Patient emrPatient) {
        if (emrPatient.isDead()) {
            emrPatient.setCauseOfDeath(getCauseOfDeath(emrPatient));
            patientService.savePatient(emrPatient);
            systemUserService.setOpenmrsShrSystemUserAsCreator(emrPatient);
        }
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

    private boolean shouldSyncEncounter(String encounterId, Bundle bundle) {
        if (idMappingsRepository.findByExternalId(encounterId) != null) {
            return false;
        }
        if (getEncounterConfidentiality(bundle).ordinal() > Confidentiality.Normal.ordinal()) {
            return false;
        }
        return true;
    }

    private Confidentiality getEncounterConfidentiality(Bundle bundle) {
        Composition composition = bundle.getAllPopulatedChildElementsOfType(Composition.class).get(0);
        String confidentialityCode = composition.getConfidentiality();
        if (null == confidentialityCode) {
            return Confidentiality.Normal;
        }
        ;
        return getConfidentiality(confidentialityCode);
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
