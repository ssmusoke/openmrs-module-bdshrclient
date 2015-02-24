package org.openmrs.module.fhir.utils;

import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.UserService;

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

    public static String extractProviderId(String providerUrl) {
        if (providerUrl == null) {
            return null;
        }
        try {
            return providerUrl.substring(providerUrl.lastIndexOf('/') + 1, providerUrl.lastIndexOf('.'));
        } catch (StringIndexOutOfBoundsException ex) {
            return null;
        }
    }
}
