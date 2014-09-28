package org.openmrs.module.shrclient.service.impl;

import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.AtomFeed;
import org.openmrs.*;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.UserService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.fhir.mapper.emr.FHIRMapper;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.dao.PatientAttributeSearchHandler;
import org.openmrs.module.shrclient.model.Address;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.service.BbsCodeService;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
public class MciPatientServiceImpl extends BaseOpenmrsService implements MciPatientService {

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
    private ProviderService providerService;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    private static final Logger logger = Logger.getLogger(MciPatientServiceImpl.class);

    @Override
    public org.openmrs.Patient createOrUpdatePatient(Patient mciPatient) {
        Integer emrPatientId = new PatientAttributeSearchHandler(Constants.HEALTH_ID_ATTRIBUTE).getUniquePatientIdFor(mciPatient.getHealthId());
        org.openmrs.Patient emrPatient = emrPatientId != null ? patientService.getPatient(emrPatientId) : new org.openmrs.Patient();

        emrPatient.setGender(mciPatient.getGender());
        setIdentifier(emrPatient);
        setPersonName(emrPatient, mciPatient);
        setPersonAddress(emrPatient, mciPatient.getAddress());

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

        setCreator(emrPatient);
        patientService.savePatient(emrPatient);
        return emrPatient;
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
    public void createOrUpdateEncounters(org.openmrs.Patient emrPatient, List<EncounterBundle> bundles) {
        for (EncounterBundle bundle : bundles) {
            try {
                updateEncounter(emrPatient, bundle);
            } catch (Exception e) {
                //TODO do proper handling, write to log API?
                System.out.println(e);
                logger.error("error Occurred while trying to process Encounter from SHR.", e);
            }
        }
    }

    private void updateEncounter(org.openmrs.Patient emrPatient, EncounterBundle encounterBundle) throws Exception {
        String fhirEncounterId = encounterBundle.getEncounterId();
        AtomFeed feed = encounterBundle.getResourceOrFeed().getFeed();
        logger.debug(String.format("Processing Encounter feed from SHR for patient[%s] with Encounter ID[%s]", encounterBundle.getHealthId(), fhirEncounterId));

        if (!shouldSyncEncounter(fhirEncounterId)) return;
        org.openmrs.Encounter newEmrEncounter = fhirMapper.map(emrPatient, feed);

        setEncounterProviderAndCreator(newEmrEncounter);
        addEncounterToIdMapping(newEmrEncounter, fhirEncounterId);
        visitService.saveVisit(newEmrEncounter.getVisit());
    }

    private void addEncounterToIdMapping(org.openmrs.Encounter newEmrEncounter, String externalUuid) {
        String internalUuid = newEmrEncounter.getUuid();
        //TODO : put the right url
        String url = "";
        idMappingsRepository.saveMapping(new IdMapping(internalUuid, externalUuid, Constants.ID_MAPPING_ENCOUNTER_TYPE, url));
    }

    private void setEncounterProviderAndCreator(org.openmrs.Encounter newEmrEncounter) {
        User systemUser = getShrClientSystemUser();
        setCreator(newEmrEncounter, systemUser);
        setCreator(newEmrEncounter.getVisit(), systemUser);

        Collection<Provider> providersByPerson = providerService.getProvidersByPerson(systemUser.getPerson());
        if ((providersByPerson != null) & !providersByPerson.isEmpty()) {
            newEmrEncounter.addProvider(encounterService.getEncounterRoleByUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID), providersByPerson.iterator().next());
        }
    }

    private boolean shouldSyncEncounter(String encounterId) {
        return (idMappingsRepository.findByExternalId(encounterId) == null) ? true : false;
    }


    private void setCreator(Visit visit, User systemUser) {
        if (visit.getCreator() == null) {
            visit.setCreator(systemUser);
        } else {
            visit.setChangedBy(systemUser);
        }
    }

    private String getConceptId(String conceptName) {
        if (conceptName == null) {
            return null;
        }
        Concept concept = Context.getConceptService().getConceptByName(conceptName);
        return concept != null ? String.valueOf(concept.getConceptId()) : null;
    }

    private void setCreator(org.openmrs.Patient emrPatient) {
        User systemUser = getShrClientSystemUser();
        if (emrPatient.getCreator() == null) {
            emrPatient.setCreator(systemUser);
        } else {
            emrPatient.setChangedBy(systemUser);
        }

    }

    private void setCreator(org.openmrs.Encounter encounter, User systemUser) {
        if (encounter.getCreator() == null) {
            encounter.setCreator(systemUser);
        } else {
            encounter.setChangedBy(systemUser);
        }
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

    private User getShrClientSystemUser() {
        UserService userService = Context.getUserService();
        return userService.getUserByUsername(Constants.SHR_CLIENT_SYSTEM_NAME);
    }

    private void setPersonAddress(org.openmrs.Patient emrPatient, Address address) {
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        PersonAddress emrPatientAddress = emrPatient.getPersonAddress();
        if (emrPatientAddress == null) {
            emrPatientAddress = new PersonAddress();
        }

        AddressHierarchyEntry division = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.getDivisionId());
        if (division != null) {
            emrPatientAddress.setStateProvince(division.getName());
        }
        AddressHierarchyEntry district = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.createUserGeneratedDistrictId());
        if (district != null) {
            emrPatientAddress.setCountyDistrict(district.getName());
        }
        AddressHierarchyEntry upazilla = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.createUserGeneratedUpazillaId());
        if (upazilla != null) {
            emrPatientAddress.setAddress3(upazilla.getName());
        }
        AddressHierarchyEntry cityCorporation = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.createUserGeneratedCityCorporationId());
        if (cityCorporation != null) {
            emrPatientAddress.setAddress2(cityCorporation.getName());
        }
        AddressHierarchyEntry union = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.createUserGeneratedWardId());
        if (union != null) {
            emrPatientAddress.setCityVillage(union.getName());
        }

        if (!"".equals(address.getAddressLine())) {
            emrPatientAddress.setAddress1(address.getAddressLine());
        }

        emrPatientAddress.setPreferred(true);
        emrPatient.addAddress(emrPatientAddress);

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
}
