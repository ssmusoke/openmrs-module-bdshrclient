package org.openmrs.module.shrclient.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.util.DatabaseUpdater;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class IdMappingsRepository {

    private Logger logger = Logger.getLogger(IdMappingsRepository.class);

    public void saveMapping(IdMapping idMapping) {
        if (!mappingExists(idMapping)) {
            String query = "insert into shr_id_mapping (internal_id, external_id, type, url) values (?,?,?,?)";
            Connection conn = null;
            PreparedStatement statement = null;
            try {
                conn = DatabaseUpdater.getConnection();
                statement = conn.prepareStatement(query);
                statement.setString(1, idMapping.getInternalId());
                statement.setString(2, idMapping.getExternalId());
                statement.setString(3, idMapping.getType());
                statement.setString(4, idMapping.getUrl());
                statement.execute();
            } catch (Exception e) {
                throw new RuntimeException("Error occurred while creating id mapping", e);
            } finally {
                try {
                    if (statement != null) statement.close();
                } catch (SQLException e) {
                    logger.warn("Could not close db statement or resultset", e);
                }
            }
        }
    }

    private boolean mappingExists(IdMapping idMapping) {
        String query = "select distinct map.internal_id from shr_id_mapping map where map.internal_id=? and map.external_id=?";
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        boolean result = false;
        try {
            conn = DatabaseUpdater.getConnection();
            statement = conn.prepareStatement(query);
            statement.setString(1, idMapping.getInternalId());
            statement.setString(2, idMapping.getExternalId());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                if (StringUtils.isNotBlank(resultSet.getString(1))) {
                    result = true;
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while querying id mapping", e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.warn("Could not close db statement or result set", e);
            }
        }
        return result;
    }

    public IdMapping findByExternalId(String uuid) {
        String query = "select distinct map.internal_id, map.type, map.url from shr_id_mapping map where map.external_id=?";
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        IdMapping result = null;
        try {
            conn = DatabaseUpdater.getConnection();
            statement = conn.prepareStatement(query);
            statement.setString(1, uuid);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                if (StringUtils.isNotBlank(resultSet.getString(1))) {
                    result = new IdMapping(resultSet.getString(1), uuid, resultSet.getString(2), resultSet.getString(3));
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while querying id mapping", e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.warn("Could not close db statement or result set", e);
            }
        }
        return result;
    }

    public IdMapping findByInternalId(String uuid) {
        String query = "select distinct map.external_id, map.type, map.url from shr_id_mapping map where map.internal_id=?";
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        IdMapping result = null;
        try {
            conn = DatabaseUpdater.getConnection();
            statement = conn.prepareStatement(query);
            statement.setString(1, uuid);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                if (StringUtils.isNotBlank(resultSet.getString(1))) {
                    result = new IdMapping(uuid, resultSet.getString(1), resultSet.getString(2), resultSet.getString(3));
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while querying id mapping", e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.warn("Could not close db statement or result set", e);
            }
        }
        return result;
    }
}
