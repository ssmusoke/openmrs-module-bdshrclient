package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.OrderCareSettingLookupService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class FHIRDiagnosticOrderMapper implements FHIRResourceMapper {
    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    @Autowired
    private ProviderLookupService providerLookupService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderCareSettingLookupService orderCareSettingLookupService;

    @Override
    public boolean canHandle(IResource resource) {
        return resource instanceof DiagnosticOrder;
    }

    @Override
    public void map(Bundle bundle, IResource resource, Patient emrPatient, Encounter encounter, Map<String, List<String>> processedList) {
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) resource;
        if (processedList.containsKey(diagnosticOrder.getIdentifier().get(0).getValue()))
            return;
        createTestOrders(bundle, diagnosticOrder, emrPatient, encounter, processedList);
    }

    private void createTestOrders(Bundle bundle, DiagnosticOrder diagnosticOrder, Patient patient, Encounter encounter, Map<String, List<String>> processedList) {
        List<DiagnosticOrder.Item> item = diagnosticOrder.getItem();
        ArrayList<String> processedTestOrderUuids = new ArrayList<>();
        for (DiagnosticOrder.Item diagnosticOrderItemComponent : item) {
            Concept testOrderConcept = omrsConceptLookup.findConcept(diagnosticOrderItemComponent.getCode().getCoding());
            if (testOrderConcept != null) {
                Order testOrder = createTestOrder(bundle, diagnosticOrder, patient, encounter, testOrderConcept);
                encounter.addOrder(testOrder);
                processedTestOrderUuids.add(testOrder.getUuid());
            }
        }
        if (!processedTestOrderUuids.isEmpty()) {
            processedList.put(diagnosticOrder.getIdentifier().get(0).getValue(), processedTestOrderUuids);
        }
    }

    private Order createTestOrder(Bundle bundle, DiagnosticOrder diagnosticOrder, Patient patient, Encounter encounter, Concept testOrderConcept) {
        Order testOrder = new Order();
        testOrder.setOrderType(orderService.getOrderTypeByName("Lab Order"));
        testOrder.setConcept(testOrderConcept);
        testOrder.setPatient(patient);
        testOrder.setEncounter(encounter);
        setOrderer(testOrder, diagnosticOrder);
        testOrder.setDateActivated(encounter.getEncounterDatetime());
        testOrder.setCareSetting(orderCareSettingLookupService.getCareSetting(bundle));
        return testOrder;
    }

    private void setOrderer(Order testOrder, DiagnosticOrder diagnosticOrder) {
        ResourceReferenceDt orderer = diagnosticOrder.getOrderer();
        if (orderer != null) {
            String practitionerReferenceUrl = orderer.getReference().getValue();
            testOrder.setOrderer(providerLookupService.getProviderByReferenceUrl(practitionerReferenceUrl));
        }
    }
}
