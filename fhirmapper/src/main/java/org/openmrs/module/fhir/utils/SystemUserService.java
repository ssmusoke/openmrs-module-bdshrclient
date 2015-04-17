package org.openmrs.module.fhir.utils;

import org.openmrs.*;
import org.openmrs.api.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SystemUserService {
    private UserService userService;

    @Autowired
    public SystemUserService(UserService userService) {
        this.userService = userService;
    }

    public User getOpenMRSDeamonUser() {
        return userService.getUserByUuid(Constants.OPENMRS_DAEMON_USER);
    }

    public void setCreator(BaseOpenmrsData openMrsEntity) {
        User systemUser = getOpenMRSDeamonUser();
        if (openMrsEntity.getCreator() == null) {
            openMrsEntity.setCreator(systemUser);
        } else {
            openMrsEntity.setChangedBy(systemUser);
        }
    }

    public boolean isUpdatedByOpenMRSDaemonUser(BaseOpenmrsData openMrsEntity) {
        User changedByUser = openMrsEntity.getChangedBy();
        if (changedByUser == null) {
            changedByUser = openMrsEntity.getCreator();
        }
        User openMrsDaemonUser = getOpenMRSDeamonUser();
        return openMrsDaemonUser.getId().equals(changedByUser.getId());
    }

}
