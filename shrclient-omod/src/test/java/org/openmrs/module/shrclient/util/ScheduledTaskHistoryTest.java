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
    private static final int OFF_SET = 100;
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
        String query = String.format(QUERY_FORMAT_TO_GET_UPDATED_SINCE, taskName);

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn(SOME_DATE_TIME_WITHOUT_JUNK);
        when(database.get(query)).thenReturn(resultSet);

        String lastExecutionDateAndTime = new ScheduledTaskHistory(database).getUpdatedSinceDateAndTime(taskName);

        verify(resultSet).next();
        verify(resultSet).getString(1);
        verify(database).get(query);

        assertEquals(SOME_DATE_TIME_WITHOUT_JUNK, lastExecutionDateAndTime);
    }

    @Test
    public void shouldSetUpdatedSinceDateAndTime() {

    }

    @Test
    public void shouldFetchOffset() throws SQLException {

        String taskName = "FR Sync Task";
        String query = String.format(QUERY_FORMAT_TO_GET_OFFSET, LR_DIVISIONS_LEVEL, taskName);

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(100);
        when(database.get(query)).thenReturn(resultSet);

        int offSet = new ScheduledTaskHistory(database).getOffset(LR_DIVISIONS_LEVEL);

        verify(resultSet).next();
        verify(resultSet).getInt(1);
        verify(database).get(query);

        assertEquals(OFF_SET, offSet);
    }

    @Test
    public void shouldSetTheOffsetInDatabase() {

        String taskName = "FR Sync Task";
        String query = String.format(QUERY_FORMAT_TO_SET_OFFSET, OFF_SET, LR_DIVISIONS_LEVEL, taskName);

        when(database.save(query)).thenReturn(true);

        Boolean isExecuted = new ScheduledTaskHistory(database).setOffset(LR_DIVISIONS_LEVEL, OFF_SET);

        verify(database).save(query);

        assertEquals(true, isExecuted);
    }

}