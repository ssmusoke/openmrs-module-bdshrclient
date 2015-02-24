package org.openmrs.module.shrclient.util;

import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.UserService;
import org.openmrs.module.fhir.utils.Constants;

public class ParticipantHelper {

    public static User getOpenMRSDeamonUser(UserService userService) {
        return userService.getUserByUuid(Constants.OPENMRS_DAEMON_USER);
    }

    public static void setCreator(Visit visit, User systemUser) {
        if (visit.getCreator() == null) {
            visit.setCreator(systemUser);
        } else {
            visit.setChangedBy(systemUser);
        }
    }

    public static void setCreator(org.openmrs.Encounter encounter, User systemUser) {
        if (encounter.getCreator() == null) {
            encounter.setCreator(systemUser);
        } else {
            encounter.setChangedBy(systemUser);
        }
    }

    public static void setCreator(org.openmrs.Patient emrPatient, UserService userService) {
        User systemUser = getOpenMRSDeamonUser(userService);
        if (emrPatient.getCreator() == null) {
            emrPatient.setCreator(systemUser);
        } else {
            emrPatient.setChangedBy(systemUser);
        }

    }
}
