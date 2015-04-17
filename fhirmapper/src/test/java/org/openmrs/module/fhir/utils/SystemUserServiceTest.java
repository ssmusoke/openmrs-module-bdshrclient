package org.openmrs.module.fhir.utils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.User;
import org.openmrs.api.UserService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.fhir.utils.Constants.OPENMRS_DAEMON_USER;

public class SystemUserServiceTest {
    @Mock
    private UserService userService;
    private SystemUserService systemUserService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        systemUserService = new SystemUserService(userService);
    }

    @Test
    public void shouldCheckIfOpenMrsDaemonUserHasUpdated() throws Exception {
        final org.openmrs.Patient openMrsPatient = new org.openmrs.Patient();
        User shrUser = new User();
        shrUser.setId(2);

        openMrsPatient.setCreator(shrUser);
        when(userService.getUserByUuid(OPENMRS_DAEMON_USER)).thenReturn(shrUser);
        assertTrue(systemUserService.isUpdatedByOpenMRSDaemonUser(openMrsPatient));

        User bahmniUser = new User();
        bahmniUser.setId(1);
        openMrsPatient.setCreator(bahmniUser);
        assertFalse(systemUserService.isUpdatedByOpenMRSDaemonUser(openMrsPatient));
    }
}