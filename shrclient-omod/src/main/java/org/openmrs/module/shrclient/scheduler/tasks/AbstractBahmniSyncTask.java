package org.openmrs.module.shrclient.scheduler.tasks;

import org.ict4h.atomfeed.client.service.EventWorker;
import org.ict4h.atomfeed.client.service.FeedClient;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.fhir.mapper.bundler.CompositionBundleCreator;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.feeds.openmrs.OpenMRSFeedClientFactory;
import org.openmrs.module.shrclient.handlers.EncounterPush;
import org.openmrs.module.shrclient.handlers.PatientPush;
import org.openmrs.module.shrclient.handlers.ServiceClientRegistry;
import org.openmrs.module.shrclient.mapper.PatientMapper;
import org.openmrs.module.shrclient.service.impl.BbsCodeServiceImpl;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.net.URISyntaxException;

public abstract class AbstractBahmniSyncTask extends AbstractTask {
    public static final String OPENMRS_PATIENT_FEED_URI = "openmrs://events/patient/recent";
    public static final String OPENMRS_ENCOUNTER_FEED_URI = "openmrs://events/encounter/recent";

    @Override
    public void execute() {
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        UserService userService = Context.getUserService();
        ServiceClientRegistry serviceClientRegistry = new ServiceClientRegistry(propertiesReader);

        PatientPush patientPush = getPatientRegistry(userService, serviceClientRegistry);
        EncounterPush encounterPush = getEncounterRegistry(propertiesReader, userService, serviceClientRegistry);

        executeBahmniTask(patientPush, encounterPush);
    }

    protected abstract void executeBahmniTask(PatientPush patientPush, EncounterPush encounterPush);

    private EncounterPush getEncounterRegistry(PropertiesReader propertiesReader, UserService userService, ServiceClientRegistry serviceClientRegistry) {
        return new EncounterPush(Context.getEncounterService(), userService,
                propertiesReader,
                PlatformUtil.getRegisteredComponent(CompositionBundleCreator.class),
                PlatformUtil.getRegisteredComponent(IdMappingsRepository.class),
                serviceClientRegistry);
    }

    private PatientPush getPatientRegistry(UserService userService, ServiceClientRegistry serviceClientRegistry) {
        return new PatientPush(
                Context.getPatientService(),
                userService,
                Context.getPersonService(),
                new PatientMapper(Context.getService(AddressHierarchyService.class), new BbsCodeServiceImpl()),
                serviceClientRegistry.getMCIClient());
    }

    protected FeedClient getFeedClient(String uri, EventWorker worker) throws URISyntaxException {
        return new OpenMRSFeedClientFactory().getFeedClient(uri, worker);
    }
}
