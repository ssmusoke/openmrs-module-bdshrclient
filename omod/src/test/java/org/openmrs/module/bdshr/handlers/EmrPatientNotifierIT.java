package org.openmrs.module.bdshr.handlers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.bdshrclient.handlers.EmrPatientNotifier;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class EmrPatientNotifierIT extends BaseModuleWebContextSensitiveTest {

    @Before
    public void setup() throws Exception {
        executeDataSet("bdShrClientTestDS.xml");
    }

    //@Test
    public void shouldProcessInternalPatientsEvent() {
        EmrPatientNotifier emrPatientNotifier = new EmrPatientNotifier();
        emrPatientNotifier.process();
    }

   
}
