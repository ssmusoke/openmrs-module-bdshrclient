package org.openmrs.module.bdshrclient.service.impl;

import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.bdshrclient.service.MciPatientService;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;

import java.util.List;


public class MciPatientServiceImpl extends BaseOpenmrsService implements MciPatientService {

    private final String identifierSrcName = "BAM";

    @Override
    public Patient createOrUpdatePatient(org.openmrs.module.bdshrclient.model.Patient mciPatient) {
        PatientService patientService = Context.getPatientService();


        return null;
    }


    public String getIdentifier() {
        IdentifierSourceService identifierSourceService = Context.getService(IdentifierSourceService.class);
        List<IdentifierSource> allIdentifierSources = identifierSourceService.getAllIdentifierSources(false);
        for (IdentifierSource identifierSource : allIdentifierSources) {
            if (((SequentialIdentifierGenerator)identifierSource).getPrefix().equals(identifierSrcName)) {
                return identifierSourceService.generateIdentifier(identifierSource, "");
            }
        }
        return null;
    }

}
