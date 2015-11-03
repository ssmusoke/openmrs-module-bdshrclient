package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Specimen;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticOrderStatusEnum;
import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Order;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.CodeableConceptService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.openmrs.module.fhir.FHIRProperties.LOINC_SOURCE_NAME;
import static org.openmrs.module.fhir.MRSProperties.MRS_LAB_ORDER_TYPE;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.findResourceByReference;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.identifyResource;

@Component("fhirTestOrderMapper")
public class TestOrderMapper implements EmrOrderResourceHandler {

    private static final int TEST_TYPE_POSITION_IN_SPLITED_CONCEPT_REF_TERM = 3;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    private CodeableConceptService codeableConceptService;
    @Autowired
    private ProviderLookupService providerLookupService;
    private List<FHIRResource> resources;

    @Override
    public boolean canHandle(Order order) {
        return order.getOrderType().getName().equalsIgnoreCase(MRS_LAB_ORDER_TYPE);
    }

    @Override
    public List<FHIRResource> map(Order order, Encounter fhirEncounter, Bundle bundle, SystemProperties systemProperties) {
        resources = new ArrayList<>();
        DiagnosticOrder diagnosticOrder;
        IResource resource = identifyResource(bundle.getEntry(), new DiagnosticOrder().getResourceName());
        if (resource != null) {
            diagnosticOrder = (DiagnosticOrder) resource;
        } else {
            diagnosticOrder = createDiagnosticOrder(order, fhirEncounter, systemProperties);
            resources.add(new FHIRResource("Diagnostic Order", diagnosticOrder.getIdentifier(), diagnosticOrder));
        }
        addItemsToDiagnosticOrder(order, diagnosticOrder, bundle, systemProperties);
        if (CollectionUtils.isEmpty(diagnosticOrder.getItem())) {
            return null;
        }
        return resources;
    }

    private void addItemsToDiagnosticOrder(Order order, DiagnosticOrder diagnosticOrder, Bundle bundle, SystemProperties systemProperties) {
        DiagnosticOrder.Item orderItem = diagnosticOrder.addItem();
        CodeableConceptDt orderCode = findOrderName(order);
        if (orderCode == null) return;
        orderItem.setCode(orderCode);
        orderItem.setStatus(DiagnosticOrderStatusEnum.REQUESTED);

        addSpecimenToDiagnosticOrder(order, diagnosticOrder, orderItem, bundle, systemProperties);
    }

    private void addSpecimenToDiagnosticOrder(Order order, DiagnosticOrder diagnosticOrder, DiagnosticOrder.Item orderItem, Bundle bundle, SystemProperties systemProperties) {
        Specimen specimen = getIfSpecimenExists(bundle, diagnosticOrder.getSpecimen(), order, systemProperties);
        if (specimen == null) {
            specimen = createSpecimen(order, diagnosticOrder, systemProperties);
            if (specimen == null) {
                return;
            }
            resources.add(new FHIRResource("Specimen", specimen.getIdentifier(), specimen));
            diagnosticOrder.addSpecimen().setReference(specimen.getId().getValue());
        }
        orderItem.addSpecimen().setReference(specimen.getId().getValue());
    }

    private Specimen getIfSpecimenExists(Bundle bundle, List<ResourceReferenceDt> specimenList, Order order, SystemProperties systemProperties) {
        for (ResourceReferenceDt resourceReference : specimenList) {
            IResource resource = findResourceByReference(bundle, resourceReference);
            if (resource != null) {
                Specimen specimenResource = (Specimen) resource;

                String type = specimenResource.getType().getCoding().get(0).getDisplay();
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
                return specimenResource.getAccessionIdentifier().getValue().equalsIgnoreCase(new EntityReference().build(Order.class, systemProperties, order.getAccessionNumber()));
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
            IdentifierDt accessionIdentifier = new IdentifierDt().setValue(new EntityReference().build(Order.class, systemProperties, order.getAccessionNumber()));
            specimen.setAccessionIdentifier(accessionIdentifier);
        }

        String id = new EntityReference().build(Order.class, systemProperties, UUID.randomUUID().toString());
        specimen.addIdentifier().setValue(id);
        specimen.setId(id);
        CodeableConceptDt codeableConcept = new CodeableConceptDt();
        CodingDt coding = codeableConcept.addCoding();
        String loincSystem = findLoincReferenceTerm(order);
        if (loincSystem == null) {
            return null;
        }
        coding.setDisplay(loincSystem);
        specimen.setType(codeableConcept);
        return specimen;
    }

    private CodeableConceptDt findOrderName(Order order) {
        if (null == order.getConcept()) {
            return null;
        }
        CodeableConceptDt result = codeableConceptService.addTRCoding(order.getConcept(), idMappingsRepository);
        if (result.getCoding() != null && result.getCoding().isEmpty()) {
            CodingDt coding = result.addCoding();
            coding.setDisplay(order.getConcept().getName().getName());
        }
        return result;
    }

    private DiagnosticOrder createDiagnosticOrder(Order order, Encounter fhirEncounter, SystemProperties systemProperties) {
        DiagnosticOrder diagnosticOrder;
        diagnosticOrder = new DiagnosticOrder();
        diagnosticOrder.setSubject(fhirEncounter.getPatient());
        diagnosticOrder.setOrderer(getOrdererReference(order, fhirEncounter, systemProperties));
        String id = new EntityReference().build(Order.class, systemProperties, UUID.randomUUID().toString());
        diagnosticOrder.addIdentifier().setValue(id);
        diagnosticOrder.setId(id);
        diagnosticOrder.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId().getValue()));
        diagnosticOrder.setStatus(DiagnosticOrderStatusEnum.REQUESTED);
        return diagnosticOrder;
    }

    private ResourceReferenceDt getOrdererReference(Order order, Encounter encounter, SystemProperties systemProperties) {
        if (order.getOrderer() != null) {
            String providerUrl = providerLookupService.getProviderRegistryUrl(systemProperties, order.getOrderer());
            if (providerUrl != null) {
                return new ResourceReferenceDt().setReference(providerUrl);
            }
        }
        List<Encounter.Participant> participants = encounter.getParticipant();
        if (!CollectionUtils.isEmpty(participants)) {
            return participants.get(0).getIndividual();
        }
        return null;
    }
}