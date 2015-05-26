package org.openmrs.module.shrclient.util;

import org.apache.log4j.Logger;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.UserService;
import org.openmrs.module.fhir.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
public class SystemUserService {
    private UserService userService;
    private Database database;
    private User daemonUser;

    private Logger logger = Logger.getLogger(SystemUserService.class);

    @Autowired
    public SystemUserService(UserService userService, Database database) {
        this.userService = userService;
        this.database = database;
    }

    public User getOpenMRSDeamonUser() {
        if (daemonUser == null) {
            daemonUser = userService.getUserByUuid(Constants.OPENMRS_DAEMON_USER);
        }
        return daemonUser;
    }

    public boolean isUpdatedByOpenMRSDaemonUser(BaseOpenmrsData openMrsEntity) {
        User changedByUser = openMrsEntity.getChangedBy();
        if (changedByUser == null) {
            changedByUser = openMrsEntity.getCreator();
        }
        User openMrsDaemonUser = getOpenMRSDeamonUser();
        return openMrsDaemonUser.getId().equals(changedByUser.getId());
    }

    public void setOpenmrsDeamonUserAsCreator(Encounter encounter) {
        updateUser("encounter", "encounter_id", encounter);
    }

    public void setOpenmrsDeamonUserAsCreator(Visit visit) {
        updateUser("visit", "visit_id", visit);
    }

    public void setOpenmrsDeamonUserAsCreator(Patient patient) {
        updateUser("patient", "patient_id", patient);
    }

    private void updateUser(final String tableName, final String idColumnName, final BaseOpenmrsData openMrsEntity) {
        database.executeInTransaction(new Database.TxWork<Object>() {
            @Override
            public Object execute(Connection connection) {
                PreparedStatement statement = null;
                String userColumnName = null;
                if (openMrsEntity.getDateChanged() != null && openMrsEntity.getDateChanged().after(openMrsEntity.getDateCreated())) {
                    userColumnName = "changed_by";
                } else {
                    userColumnName = "creator";
                }
                String query = "update " + tableName + " set " + userColumnName + " = ? where " + idColumnName + " = ?;";
                try {
                    statement = connection.prepareStatement(query);
                    statement.setInt(1, getOpenMRSDeamonUser().getUserId());
                    statement.setInt(2, openMrsEntity.getId());
                    statement.executeUpdate();
                } catch (Exception e) {
                    throw new RuntimeException("Error occurred while updating " + tableName, e);
                } finally {
                    try {
                        if (statement != null) statement.close();
                    } catch (SQLException e) {
                        logger.warn("Could not close db statement or resultset", e);
                    }
                }
                return null;
            }
        });
    }
}
