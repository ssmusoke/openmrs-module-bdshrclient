package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.DiagnosticOrder;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceReference;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.TestOrder;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.OrderCareSettingLookupService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.shrclient.util.ConceptCache;
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
    public boolean canHandle(Resource resource) {
        return resource instanceof DiagnosticOrder;
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter encounter, Map<String, List<String>> processedList, ConceptCache conceptCache) {
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) resource;
        if (processedList.containsKey(diagnosticOrder.getIdentifier().get(0).getValueSimple()))
            return;
        createTestOrders(feed, diagnosticOrder, emrPatient, encounter, processedList);
    }

    private void createTestOrders(AtomFeed feed, DiagnosticOrder diagnosticOrder, Patient patient, Encounter encounter, Map<String, List<String>> processedList) {
        List<DiagnosticOrder.DiagnosticOrderItemComponent> item = diagnosticOrder.getItem();
        ArrayList<String> processedTestOrderUuids = new ArrayList<>();
        for (DiagnosticOrder.DiagnosticOrderItemComponent diagnosticOrderItemComponent : item) {
            Concept testOrderConcept = omrsConceptLookup.findConcept(diagnosticOrderItemComponent.getCode().getCoding());
            if (testOrderConcept != null) {
                TestOrder testOrder = createTestOrder(feed, diagnosticOrder, patient, encounter, testOrderConcept);
                encounter.addOrder(testOrder);
                processedTestOrderUuids.add(testOrder.getUuid());
            }
        }
        if (!processedTestOrderUuids.isEmpty()) {
            processedList.put(diagnosticOrder.getIdentifier().get(0).getValueSimple(), processedTestOrderUuids);
        }
    }

    private TestOrder createTestOrder(AtomFeed feed, DiagnosticOrder diagnosticOrder, Patient patient, Encounter encounter, Concept testOrderConcept) {
        TestOrder testOrder = new TestOrder();
        testOrder.setOrderType(orderService.getOrderTypeByName("Lab Order"));
        testOrder.setConcept(testOrderConcept);
        testOrder.setPatient(patient);
        testOrder.setEncounter(encounter);
        setOrderer(testOrder, diagnosticOrder);
        testOrder.setDateActivated(encounter.getEncounterDatetime());
        testOrder.setCareSetting(orderCareSettingLookupService.getCareSetting(feed));
        return testOrder;
    }

    private void setOrderer(TestOrder testOrder, DiagnosticOrder diagnosticOrder) {
        ResourceReference orderer = diagnosticOrder.getOrderer();
        if (orderer != null) {
            String practitionerReferenceUrl = orderer.getReferenceSimple();
            testOrder.setOrderer(providerLookupService.getProviderByReferenceUrl(practitionerReferenceUrl));
        }
    }
}
