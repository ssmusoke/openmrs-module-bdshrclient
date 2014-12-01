package org.openmrs.module.shrclient.advice;

import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.transaction.AFTransactionWork;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.openmrs.Encounter;
import org.openmrs.api.EncounterService;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;

import java.lang.reflect.Method;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ShrEncounterAdviceTest {

    @Mock
    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
    @Mock
    private EventService eventService;

    private ArgumentCaptor<AFTransactionWorkWithoutResult> captor = ArgumentCaptor.forClass(AFTransactionWorkWithoutResult.class);

    private ShrEncounterAdvice encounterSaveInterceptor;

    private Encounter encounter;

    @Before
    public void setup() {
        initMocks(this);
        encounterSaveInterceptor = new ShrEncounterAdvice(atomFeedSpringTransactionManager, eventService);
        encounter = new Encounter();
        encounter.setUuid("uuid");
    }

    @Test
    public void shouldPublishUpdateEventToFeedAfterSaveEncounter() throws Throwable {
        Method method = EncounterService.class.getMethod("saveEncounter", Encounter.class);
        Object[] objects = new Object[]{encounter};

        encounterSaveInterceptor.afterReturning(encounter, method, objects, null);
        verify(atomFeedSpringTransactionManager).executeWithTransaction(any(AFTransactionWorkWithoutResult.class));
    }

    @Test
    public void shouldSaveEventInTheSameTransactionAsTheTrigger() throws Throwable {
        Method method = EncounterService.class.getMethod("saveEncounter", Encounter.class);
        Object[] objects = new Object[]{encounter};

        encounterSaveInterceptor.afterReturning(encounter, method, objects, null);
        verify(atomFeedSpringTransactionManager).executeWithTransaction(captor.capture());

        assertEquals(AFTransactionWork.PropagationDefinition.PROPAGATION_REQUIRED, captor.getValue().getTxPropagationDefinition());
    }


}