package org.openmrs.module.shrclient.dao;

import org.apache.log4j.Logger;
import org.openmrs.util.DatabaseUpdater;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class PatientAttributeSearchHandler {

    private String attrName;

    private Logger logger = Logger.getLogger(PatientAttributeSearchHandler.class);

    public PatientAttributeSearchHandler(String attrName) {
        this.attrName = attrName;
    }

    public Integer getUniquePatientIdFor(String attrValue) {
        String query = "select distinct p.patient_id from person_attribute_type pat" +
                " join person_attribute pa on pa.person_attribute_type_id = pat.person_attribute_type_id" +
                " join patient p on pa.person_id = p.patient_id" +
                " where pat.name = ? and pa.value = ?";
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Integer emrPatientId = null;
        try {
            conn = DatabaseUpdater.getConnection();
            statement = conn.prepareStatement(query);
            statement.setString(1, this.attrName);
            statement.setString(2, attrValue);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                emrPatientId = Integer.valueOf(resultSet.getInt(1));
                break;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while identifying Patient", e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.warn("Could not close db statement or resultset", e);
            }
        }
        return emrPatientId;
    }
}
