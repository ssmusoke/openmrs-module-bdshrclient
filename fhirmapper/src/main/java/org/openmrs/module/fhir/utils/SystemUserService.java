package org.openmrs.module.fhir.utils;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SystemUserService {

    @Autowired
    private UserService userService;

    private User getOpenMRSDeamonUser() {
        return userService.getUserByUuid(Constants.OPENMRS_DAEMON_USER);
    }

    public void setCreator(Visit visit) {
        User systemUser = getOpenMRSDeamonUser();
        if (visit.getCreator() == null) {
            visit.setCreator(systemUser);
        } else {
            visit.setChangedBy(systemUser);
        }
    }

    public void setCreator(Encounter encounter) {
        User systemUser = getOpenMRSDeamonUser();
        if (encounter.getCreator() == null) {
            encounter.setCreator(systemUser);
        } else {
            encounter.setChangedBy(systemUser);
        }
    }

    public void setCreator(Patient emrPatient) {
        User systemUser = getOpenMRSDeamonUser();
        if (emrPatient.getCreator() == null) {
            emrPatient.setCreator(systemUser);
        } else {
            emrPatient.setChangedBy(systemUser);
        }

    }
}
