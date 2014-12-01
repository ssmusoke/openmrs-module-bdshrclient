package org.openmrs.module.shrclient.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component("bdShrClientIdMappingRepository")
public class IdMappingsRepository {

    private Logger logger = Logger.getLogger(IdMappingsRepository.class);


    private PlatformTransactionManager getTransactionManager() {
        final List<PlatformTransactionManager> registeredComponents = Context.getRegisteredComponents(PlatformTransactionManager.class);
        return registeredComponents.get(0);
    }


    private interface TxWork<T> {
        T execute(Connection connection);
    }
    private <T> T executeInTransaction(final TxWork<T> work) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(getTransactionManager());
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return transactionTemplate.execute(new TransactionCallback<T>() {
            @Override
            public T doInTransaction(TransactionStatus transactionStatus) {
                return work.execute(getConnection());
            }
        });
    }

    private Connection getConnection() {
        ServiceContext serviceContext = ServiceContext.getInstance();
        Class klass = serviceContext.getClass();
        try {
            Field field = klass.getDeclaredField("applicationContext");
            field.setAccessible(true);
            ApplicationContext applicationContext = (ApplicationContext) field.get(serviceContext);
            SessionFactory factory = (SessionFactory) applicationContext.getBean("sessionFactory");
            return factory.getCurrentSession().connection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveMapping(final IdMapping idMapping) {
        executeInTransaction(new TxWork<Object>() {
            @Override
            public Object execute(Connection connection) {
                if (!mappingExists(idMapping)) {
                    String query = "insert into shr_id_mapping (internal_id, external_id, type, uri) values (?,?,?,?)";
                    PreparedStatement statement = null;
                    try {
                        statement = connection.prepareStatement(query);
                        statement.setString(1, idMapping.getInternalId());
                        statement.setString(2, idMapping.getExternalId());
                        statement.setString(3, idMapping.getType());
                        statement.setString(4, idMapping.getUri());
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
                return null;
            }
        });
    }


    private boolean mappingExists(final IdMapping idMapping) {
        return executeInTransaction(new TxWork<Boolean>() {
            @Override
            public Boolean execute(Connection connection) {
                String query = "select distinct map.internal_id from shr_id_mapping map where map.internal_id=? and map.external_id=?";
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                boolean result = false;
                try {
                    statement = connection.prepareStatement(query);
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
        });
    }

    public IdMapping findByExternalId(final String uuid) {
        return executeInTransaction(new TxWork<IdMapping>() {
            @Override
            public IdMapping execute(Connection connection) {
                String query = "select distinct map.internal_id, map.type, map.uri from shr_id_mapping map where map.external_id=?";
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                IdMapping result = null;
                try {
                    statement = connection.prepareStatement(query);
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
        });
    }

    public IdMapping findByInternalId(final String uuid) {
        return executeInTransaction(new TxWork<IdMapping>() {
            @Override
            public IdMapping execute(Connection connection) {
                String query = "select distinct map.external_id, map.type, map.uri from shr_id_mapping map where map.internal_id=?";
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                IdMapping result = null;
                try {
                    statement = connection.prepareStatement(query);
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
        });
    }

}
