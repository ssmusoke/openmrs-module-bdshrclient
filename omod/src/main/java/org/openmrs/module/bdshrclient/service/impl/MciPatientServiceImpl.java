package org.openmrs.module.bdshrclient.service.impl;

import org.openmrs.*;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.bdshrclient.service.MciPatientService;
import org.openmrs.module.bdshrclient.util.GenderEnum;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;

import java.util.List;


public class MciPatientServiceImpl extends BaseOpenmrsService implements MciPatientService {

    private final String IDENTIFIER_SOURCE_NAME = "BAM";
    public static final String EMR_PRIMARY_IDENTIFIER_TYPE = "emr.primaryIdentifierType";

    @Override
    public Patient createOrUpdatePatient(org.openmrs.module.bdshrclient.model.Patient mciPatient) {
        PatientService patientService = Context.getPatientService();
        PersonService personService = Context.getPersonService();
        Patient newPatient = new Patient();
        newPatient.setGender(GenderEnum.forCode(mciPatient.getGender()).name());
        PersonName personName = new PersonName(mciPatient.getFirstName(), mciPatient.getMiddleName(), mciPatient.getLastName());
        personName.setPreferred(true);
        newPatient.addName(personName);

        PersonAttribute nationalIdAttr = new PersonAttribute();
        PersonAttributeType nationalIdAttrType = personService.getPersonAttributeTypeByName("National ID");
        nationalIdAttr.setAttributeType(nationalIdAttrType);
        nationalIdAttr.setValue(mciPatient.getNationalId());

//        PersonAttribute healthIdAttr = new PersonAttribute();
//        PersonAttributeType healthIdAttrType = personService.getPersonAttributeTypeByName("Health ID");
//        healthIdAttr.setAttributeType(healthIdAttrType);
//        healthIdAttr.setValue(mciPatient.getHealthId());

        PatientIdentifier identifier = generateIdentifier();
        identifier.setPreferred(true);
        newPatient.addIdentifier(identifier);
        return newPatient;
    }

    private PatientIdentifierType getPatientIdentifierType() {
        AdministrationService administrationService = Context.getAdministrationService();
        String globalProperty = administrationService.getGlobalProperty(EMR_PRIMARY_IDENTIFIER_TYPE);
        PatientIdentifierType patientIdentifierByUuid = Context.getPatientService().getPatientIdentifierTypeByUuid(globalProperty);
        return patientIdentifierByUuid;
    }


    public PatientIdentifier generateIdentifier() {
        IdentifierSourceService identifierSourceService = Context.getService(IdentifierSourceService.class);
        List<IdentifierSource> allIdentifierSources = identifierSourceService.getAllIdentifierSources(false);
        for (IdentifierSource identifierSource : allIdentifierSources) {
            if (((SequentialIdentifierGenerator)identifierSource).getPrefix().equals(IDENTIFIER_SOURCE_NAME)) {
                String identifier = identifierSourceService.generateIdentifier(identifierSource, "MCI Patient");
                PatientIdentifierType identifierType = getPatientIdentifierType();
                return new PatientIdentifier(identifier, identifierType, null);
            }
        }
        return null;
    }

}
