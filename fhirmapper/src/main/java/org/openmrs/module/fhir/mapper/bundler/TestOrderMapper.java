package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Encounter;
import org.openmrs.*;
import org.openmrs.ConceptMap;
import org.openmrs.Order;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.CodableConceptService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.hl7.fhir.instance.model.DiagnosticOrder.DiagnosticOrderStatus.requested;
import static org.openmrs.module.fhir.mapper.FHIRProperties.LOINC_SOURCE_NAME;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_LAB_ORDER_TYPE;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.findResourceByReference;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.identifyResource;

@Component("fhirTestOrderMapper")
public class TestOrderMapper implements EmrOrderResourceHandler {

    private static final int TEST_TYPE_POSITION_IN_SPLITED_CONCEPT_REF_TERM = 3;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    private CodableConceptService codableConceptService;
    private List<FHIRResource> resources;

    @Override
    public boolean canHandle(Order order) {
        return (order instanceof TestOrder) && order.getOrderType().getName().equalsIgnoreCase(MRS_LAB_ORDER_TYPE);
    }

    @Override
    public List<FHIRResource> map(Order order, Encounter fhirEncounter, AtomFeed feed, SystemProperties systemProperties) {
        resources = new ArrayList<>();
        DiagnosticOrder diagnosticOrder;
        Resource resource = identifyResource(feed.getEntryList(), ResourceType.DiagnosticOrder);
        if (resource != null) {
            diagnosticOrder = (DiagnosticOrder) resource;
        } else {
            diagnosticOrder = createDiagnosticOrder(order, fhirEncounter, systemProperties);
            resources.add(new FHIRResource("Diagnostic Order", diagnosticOrder.getIdentifier(), diagnosticOrder));
        }
        addItemsToDiagnosticOrder(order, diagnosticOrder, feed, systemProperties);
        if (CollectionUtils.isEmpty(diagnosticOrder.getItem())) {
            return null;
        }
        return resources;
    }

    private void addItemsToDiagnosticOrder(Order order, DiagnosticOrder diagnosticOrder, AtomFeed feed, SystemProperties systemProperties) {
        DiagnosticOrder.DiagnosticOrderItemComponent orderItem = diagnosticOrder.addItem();
        CodeableConcept orderCode = findOrderName(order);
        if (orderCode == null) return;
        orderItem.setCode(orderCode);
        orderItem.setStatusSimple(requested);

        addSpecimenToDiagnosticOrder(order, diagnosticOrder, orderItem, feed, systemProperties);
    }

    private void addSpecimenToDiagnosticOrder(Order order, DiagnosticOrder diagnosticOrder, DiagnosticOrder.DiagnosticOrderItemComponent orderItem, AtomFeed feed, SystemProperties systemProperties) {
        Specimen specimen = getIfSpecimenExists(feed, diagnosticOrder.getSpecimen(), order, systemProperties);
        if (specimen == null) {
            specimen = createSpecimen(order, diagnosticOrder, systemProperties);
            if (specimen == null) {
                return;
            }
            resources.add(new FHIRResource("Specimen", specimen.getIdentifier(), specimen));
        }
        String specimenIdentifier = specimen.getIdentifier().get(0).getValueSimple();
        ResourceReference orderItemSpecimenReference = orderItem.addSpecimen();
        orderItemSpecimenReference.setReferenceSimple(specimenIdentifier);
        ResourceReference diagnosticOrderSpecimenReference = diagnosticOrder.addSpecimen();
        diagnosticOrderSpecimenReference.setReferenceSimple(specimenIdentifier);
    }

    private Specimen getIfSpecimenExists(AtomFeed feed, List<ResourceReference> specimenList, Order order, SystemProperties systemProperties) {
        for (ResourceReference resourceReference : specimenList) {
            Resource resource = findResourceByReference(feed, resourceReference);
            if (resource != null) {
                Specimen specimenResource = (Specimen) resource;

                String type = specimenResource.getType().getCoding().get(0).getDisplaySimple();
                //TODO find based for all Loinc sources
                String specimen = findLoincReferenceTerm(order);
                if (shouldUseSameSpecimen(order, specimenResource, type, specimen, systemProperties)) {
                    return specimenResource;
                }
            }
        }
        return null;
    }

    private boolean shouldUseSameSpecimen(Order order, Specimen specimenResource, String type, String specimen, SystemProperties systemProperties) {
        if (specimen != null && specimen.equalsIgnoreCase(type)) {
            if (order.getAccessionNumber() != null && specimenResource.getAccessionIdentifier() != null) {
                return specimenResource.getAccessionIdentifier().getValueSimple().equalsIgnoreCase(new EntityReference().build(Order.class, systemProperties, order.getAccessionNumber()));
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

    private Specimen createSpecimen(Order order, DiagnosticOrder diagnosticOrder, SystemProperties systemProperties) {
        Specimen specimen = new Specimen();
        specimen.setSubject(diagnosticOrder.getSubject());

        if (order.getAccessionNumber() != null) {
            Identifier accessionIdentifier = new Identifier();
            accessionIdentifier.setValueSimple(new EntityReference().build(Order.class, systemProperties, order.getAccessionNumber()));
            specimen.setAccessionIdentifier(accessionIdentifier);
        }

        Identifier identifier = specimen.addIdentifier();
        identifier.setValueSimple(new EntityReference().build(Order.class, systemProperties, UUID.randomUUID().toString()));
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
        CodeableConcept result = codableConceptService.addTRCoding(order.getConcept(), idMappingsRepository);
        if(result.getCoding().isEmpty()) {
            Coding coding = result.addCoding();
            coding.setDisplaySimple(order.getConcept().getName().getName());
        }
        return result;
    }

    private DiagnosticOrder createDiagnosticOrder(Order order, Encounter fhirEncounter, SystemProperties systemProperties) {
        DiagnosticOrder diagnosticOrder;
        diagnosticOrder = new DiagnosticOrder();
        diagnosticOrder.setSubject(fhirEncounter.getSubject());
        diagnosticOrder.setOrderer(getOrdererReference(order));
        Identifier identifier = diagnosticOrder.addIdentifier();
        identifier.setValueSimple(new EntityReference().build(Order.class, systemProperties, UUID.randomUUID().toString()));
        diagnosticOrder.setEncounter(fhirEncounter.getIndication());
        diagnosticOrder.setStatusSimple(requested);
        return diagnosticOrder;
    }

    //TODO : how do we identify this individual?
    private ResourceReference getOrdererReference(Order order) {
        ResourceReference orderer = new ResourceReference();
        orderer.setReferenceSimple(order.getOrderer().getUuid());
        return orderer;
    }

}