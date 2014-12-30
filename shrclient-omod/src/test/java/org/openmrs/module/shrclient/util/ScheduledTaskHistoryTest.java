package org.openmrs.module.shrclient.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.shrclient.util.ScheduledTaskHistory.*;

public class ScheduledTaskHistoryTest {

    public static final String SOME_DATE_TIME_WITHOUT_JUNK = "Some DateTime";
    private static final String OFF_SET = "100";
    private static final String LR_DIVISIONS_LEVEL = "lr.divisions";

    @Mock
    Database database;

    @Mock
    ResultSet resultSet;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldFetchUpdatedSinceDateAndTime() throws SQLException {
        String taskName = "LR Sync Task";
        String query = String.format(QUERY_FORMAT_TO_GET_FEED_URI_FOR_LAST_READ_ENTRY, taskName);

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn(SOME_DATE_TIME_WITHOUT_JUNK);
        when(database.get(query)).thenReturn(resultSet);

        String lastExecutionDateAndTime = new ScheduledTaskHistory(database).getFeedUriForLastReadEntryByFeedUri(taskName);

        verify(resultSet).next();
        verify(resultSet).getString(1);
        verify(database).get(query);

        assertEquals(SOME_DATE_TIME_WITHOUT_JUNK, lastExecutionDateAndTime);
    }

    @Test
    public void shouldSetTheOffsetInDatabase() {

        String taskName = "FR Sync Task";
        String query = String.format(QUERY_FORMAT_TO_SET_LAST_READ_ENTRY_ID, OFF_SET, LR_DIVISIONS_LEVEL, taskName);

        when(database.save(query)).thenReturn(true);

        Boolean isExecuted = new ScheduledTaskHistory(database).setLastReadEntryId(OFF_SET, LR_DIVISIONS_LEVEL);

        verify(database).save(query);

        assertEquals(true, isExecuted);
    }
}