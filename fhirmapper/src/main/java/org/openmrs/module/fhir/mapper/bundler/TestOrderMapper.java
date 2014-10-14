package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Encounter;
import org.openmrs.*;
import org.openmrs.ConceptMap;
import org.openmrs.Order;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.openmrs.module.fhir.mapper.FHIRProperties.LOINC_SOURCE_NAME;

@Component("fhirTestOrderMapper")
public class TestOrderMapper implements EmrOrderResourceHandler {

    private static final int TEST_TYPE_POSITION_IN_SPLITED_CONCEPT_REF_TERM = 3;
    @Autowired
    private IdMappingsRepository idMappingsRepository;

    private List<EmrResource> resources;

    @Override
    public boolean handles(Order order) {
        return (order instanceof TestOrder) && order.getOrderType().getName().equals("Lab Order");
    }

    @Override
    public List<EmrResource> map(Order order, Encounter fhirEncounter, AtomFeed feed) {
        resources = new ArrayList<EmrResource>();
        DiagnosticOrder diagnosticOrder;
        Resource resource = FHIRFeedHelper.identifyResource(feed.getEntryList(), ResourceType.DiagnosticOrder);
        if (resource != null) {
            diagnosticOrder = (DiagnosticOrder) resource;
        } else {
            diagnosticOrder = createDiagnosticOrder(order, fhirEncounter);
            resources.add(new EmrResource("Diagnostic Order", diagnosticOrder.getIdentifier(), diagnosticOrder));
        }
        addItemsToDiagnosticOrder(order, diagnosticOrder, feed);
        if (CollectionUtils.isEmpty(diagnosticOrder.getItem())) {
            return null;
        }
        return resources;
    }

    private void addItemsToDiagnosticOrder(Order order, DiagnosticOrder diagnosticOrder, AtomFeed feed) {
        DiagnosticOrder.DiagnosticOrderItemComponent orderItem = diagnosticOrder.addItem();
        CodeableConcept orderCode = findOrderName(order);
        if (orderCode == null) return;
        orderItem.setCode(orderCode);
        orderItem.setStatusSimple(DiagnosticOrder.DiagnosticOrderStatus.requested);

        addSpecimenToDiagnosticOrder(order, diagnosticOrder, orderItem, feed);
    }

    private void addSpecimenToDiagnosticOrder(Order order, DiagnosticOrder diagnosticOrder, DiagnosticOrder.DiagnosticOrderItemComponent orderItem, AtomFeed feed) {
        Specimen specimen = checkIfSpecimenExists(feed, diagnosticOrder.getSpecimen(), order);
        if (specimen == null) {
            specimen = getSpecimen(order, diagnosticOrder);
            if (specimen == null) {
                return;
            }
            resources.add(new EmrResource("Specimen", specimen.getIdentifier(), specimen));
        }
        String specimentIdentifier = specimen.getIdentifier().get(0).getValueSimple();
        ResourceReference orderItemSpecimenReference = orderItem.addSpecimen();
        orderItemSpecimenReference.setReferenceSimple(specimentIdentifier);
        ResourceReference diagnosticOrderSpecimenReference = diagnosticOrder.addSpecimen();
        diagnosticOrderSpecimenReference.setReferenceSimple(specimentIdentifier);
    }

    private Specimen checkIfSpecimenExists(AtomFeed feed, List<ResourceReference> specimenList, Order order) {
        for (ResourceReference resourceReference : specimenList) {
            Resource resource = FHIRFeedHelper.findResourceByReference(feed, resourceReference);
            if (resource != null) {
                Specimen specimenResource = (Specimen) resource;

                String type = specimenResource.getType().getCoding().get(0).getDisplaySimple();
                //TODO find based for all Loinc sources
                String specimen = findLoincReferenceTerm(order);
                if (shouldUseSameSpecimen(order, specimenResource, type, specimen)) {
                    return specimenResource;
                }
            }
        }
        return null;
    }

    private boolean shouldUseSameSpecimen(Order order, Specimen specimenResource, String type, String specimen) {
        if (specimen != null && specimen.equalsIgnoreCase(type)) {
            if (order.getAccessionNumber() != null && specimenResource.getAccessionIdentifier() != null) {
                return specimenResource.getAccessionIdentifier().getValueSimple().equalsIgnoreCase(order.getAccessionNumber());
            }
            return true;
        }
        return false;
    }

    private String findLoincReferenceTerm(Order order) {
        Concept concept = order.getConcept();
        Collection<ConceptMap> conceptMappings = concept.getConceptMappings();
        for (ConceptMap conceptMapping : conceptMappings) {
            ConceptReferenceTerm conceptReferenceTerm = conceptMapping.getConceptReferenceTerm();
            if (isSyncedFromTrAndLoincSource(conceptReferenceTerm)) {
                String conceptReferenceTermName = conceptReferenceTerm.getName();
                String[] split = conceptReferenceTermName.split(":");
                if (split != null && split.length > TEST_TYPE_POSITION_IN_SPLITED_CONCEPT_REF_TERM) {
                    return split[TEST_TYPE_POSITION_IN_SPLITED_CONCEPT_REF_TERM];
                }
            }
        }
        return null;
    }

    private boolean isSyncedFromTrAndLoincSource(ConceptReferenceTerm conceptReferenceTerm) {
        return idMappingsRepository.findByInternalId(conceptReferenceTerm.getUuid()) != null &&
                conceptReferenceTerm.getConceptSource().getName().startsWith(LOINC_SOURCE_NAME);
    }

    private Specimen getSpecimen(Order order, DiagnosticOrder diagnosticOrder) {
        Specimen specimen = new Specimen();
        specimen.setSubject(diagnosticOrder.getSubject());

        if (order.getAccessionNumber() != null) {
            Identifier accessionIdentifier = new Identifier();
            accessionIdentifier.setValueSimple(order.getAccessionNumber());
            specimen.setAccessionIdentifier(accessionIdentifier);
        }

        Identifier identifier = specimen.addIdentifier();
        identifier.setValueSimple(UUID.randomUUID().toString());
//        Sending order date activated as only ELIS - MRS sync is done. Needs to be changed when we order from MRS
        specimen.setReceivedTimeSimple(new DateAndTime(order.getDateActivated()));
        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = codeableConcept.addCoding();
        String loincSystem = findLoincReferenceTerm(order);
        if (loincSystem == null) {
            return null;
        }
        coding.setDisplaySimple(loincSystem);
        specimen.setType(codeableConcept);
        return specimen;
    }

    private CodeableConcept findOrderName(Order order) {
        if (null == order.getConcept()) {
            return null;
        }
        CodeableConcept result = FHIRFeedHelper.addReferenceCodes(order.getConcept(), idMappingsRepository);
        if(result.getCoding().isEmpty()) {
            Coding coding = result.addCoding();
            coding.setDisplaySimple(order.getConcept().getName().getName());
        }
        return result;
    }

    private DiagnosticOrder createDiagnosticOrder(Order order, Encounter fhirEncounter) {
        DiagnosticOrder diagnosticOrder;
        diagnosticOrder = new DiagnosticOrder();
        diagnosticOrder.setSubject(fhirEncounter.getSubject());
        ResourceReference orderer = new ResourceReference();
        orderer.setReferenceSimple(order.getOrderer().getUuid());
        diagnosticOrder.setOrderer(orderer);
        Identifier identifier = diagnosticOrder.addIdentifier();
        identifier.setValueSimple(UUID.randomUUID().toString());
        diagnosticOrder.setEncounter(fhirEncounter.getIndication());
        diagnosticOrder.setStatusSimple(DiagnosticOrder.DiagnosticOrderStatus.requested);
        return diagnosticOrder;
    }

}