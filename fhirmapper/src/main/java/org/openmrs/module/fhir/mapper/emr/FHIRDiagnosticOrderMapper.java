package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.DiagnosticOrder;
import org.hl7.fhir.instance.model.Resource;
import org.openmrs.*;
import org.openmrs.api.OrderService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.module.fhir.utils.OMRSHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CARE_SETTING_FOR_INPATIENT;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CARE_SETTING_FOR_OUTPATIENT;


@Component
public class FHIRDiagnosticOrderMapper implements FHIRResource {
    @Autowired
    private OMRSHelper omrsHelper;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private OrderService orderService;

    @Override
    public boolean canHandle(Resource resource) {
        return resource instanceof DiagnosticOrder;
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter encounter, HashMap<String, String> processedList) {
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) resource;
        createTestOrders(feed, diagnosticOrder, emrPatient, encounter);
    }

    private void createTestOrders(AtomFeed feed, DiagnosticOrder diagnosticOrder, Patient patient, Encounter encounter) {
        List<DiagnosticOrder.DiagnosticOrderItemComponent> item = diagnosticOrder.getItem();
        for (DiagnosticOrder.DiagnosticOrderItemComponent diagnosticOrderItemComponent : item) {
            Concept testOrderConcept = omrsHelper.findConcept(diagnosticOrderItemComponent.getCode().getCoding());
            if (testOrderConcept != null) {
                createTestOrder(feed, diagnosticOrder, patient, encounter, testOrderConcept);
            }
        }
    }

    private void createTestOrder(AtomFeed feed, DiagnosticOrder diagnosticOrder, Patient patient, Encounter encounter, Concept testOrderConcept) {
        TestOrder testOrder = new TestOrder();
        testOrder.setOrderType(orderService.getOrderTypeByName("Lab Order"));
        testOrder.setConcept(testOrderConcept);
        testOrder.setPatient(patient);
        testOrder.setEncounter(encounter);
        testOrder.setOrderer(getShrClientSystemProvider());
        testOrder.setDateActivated(encounter.getEncounterDatetime());
        testOrder.setCareSetting(orderService.getCareSettingByName(getCareSetting(feed, diagnosticOrder)));
        encounter.addOrder(testOrder);
    }

    private String getCareSetting(AtomFeed feed, DiagnosticOrder diagnosticOrder) {
        org.hl7.fhir.instance.model.Encounter fhirEncounter = (org.hl7.fhir.instance.model.Encounter) FHIRFeedHelper.findResourceByReference(feed, diagnosticOrder.getEncounter());
        org.hl7.fhir.instance.model.Enumeration<org.hl7.fhir.instance.model.Encounter.EncounterClass> fhirEncounterClass = fhirEncounter.getClass_();
        return fhirEncounterClass.getValue().equals(org.hl7.fhir.instance.model.Encounter.EncounterClass.inpatient) ? MRS_CARE_SETTING_FOR_INPATIENT : MRS_CARE_SETTING_FOR_OUTPATIENT;
    }

    private Provider getShrClientSystemProvider() {
        User systemUser = getShrClientSystemUser();
        Collection<Provider> providersByPerson = providerService.getProvidersByPerson(systemUser.getPerson());
        if ((providersByPerson != null) & !providersByPerson.isEmpty()) {
            return providersByPerson.iterator().next();
        }
        return null;
    }

    private User getShrClientSystemUser() {
        UserService userService = Context.getUserService();
        return userService.getUserByUsername(Constants.SHR_CLIENT_SYSTEM_NAME);
    }

}
