package org.openmrs.module.shrclient.advice;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.ict4h.atomfeed.server.repository.jdbc.AllEventRecordsJdbcImpl;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.server.service.EventServiceImpl;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.joda.time.DateTime;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class ShrEncounterAdvice implements AfterReturningAdvice {

    private static final Logger log = Logger.getLogger(ShrEncounterAdvice.class);
    public static final String ENCOUNTER_REST_URL = "/openmrs/ws/rest/v1/encounter/%s?v=custom:(uuid,encounterType,patient,visit,orders:(uuid,orderType,concept,voided))";
    public static final String TITLE = "Encounter";
    public static final String CATEGORY = "Encounter";
    private static final String SAVE_METHOD = "saveEncounter";
    private final EventService eventService;
    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;

    public ShrEncounterAdvice() {
        atomFeedSpringTransactionManager = findTransactionManager();
        eventService = getEventService(atomFeedSpringTransactionManager);
    }

    public ShrEncounterAdvice(AtomFeedSpringTransactionManager atomFeedSpringTransactionManager, EventService eventService) {
        this.atomFeedSpringTransactionManager = atomFeedSpringTransactionManager;
        this.eventService = eventService;
    }

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        if (!method.getName().equals(SAVE_METHOD)) return;
        Object encounterUuid = PropertyUtils.getProperty(returnValue, "uuid");
        if (encounterUuid == null) return;

        String url = String.format(ENCOUNTER_REST_URL, encounterUuid);
        final Event event = new Event(UUID.randomUUID().toString(), TITLE, DateTime.now(), (URI) null, url, CATEGORY);

        atomFeedSpringTransactionManager.executeWithTransaction(
                new AFTransactionWorkWithoutResult() {
                    @Override
                    protected void doInTransaction() {
                        eventService.notify(event);
                    }

                    @Override
                    public PropagationDefinition getTxPropagationDefinition() {
                        return PropagationDefinition.PROPAGATION_REQUIRED;
                    }
                }
        );
    }

    private AtomFeedSpringTransactionManager findTransactionManager() {
        PlatformTransactionManager platformTransactionManager = getSpringPlatformTransactionManager();
        return new AtomFeedSpringTransactionManager(platformTransactionManager);
    }

    private EventServiceImpl getEventService(AtomFeedSpringTransactionManager atomFeedSpringTransactionManager) {
        AllEventRecordsJdbcImpl records = new AllEventRecordsJdbcImpl(atomFeedSpringTransactionManager);
        return new EventServiceImpl(records);
    }

    private PlatformTransactionManager getSpringPlatformTransactionManager() {
        List<PlatformTransactionManager> platformTransactionManagers = Context.getRegisteredComponents(PlatformTransactionManager.class);
        return platformTransactionManagers.get(0);
    }
}
