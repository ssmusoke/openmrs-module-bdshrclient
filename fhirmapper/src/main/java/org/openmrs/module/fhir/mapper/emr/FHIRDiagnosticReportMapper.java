package org.openmrs.module.fhir.mapper.emr;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.DiagnosticReport;
import org.hl7.fhir.instance.model.Observation;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceReference;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.module.fhir.utils.OMRSHelper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_CLASS_LAB_SET;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_LAB_NOTES;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.findResourceByReference;

@Component
public class FHIRDiagnosticReportMapper implements FHIRResource {
    @Autowired
    private OMRSHelper omrsHelper;
    @Autowired
    private FHIRObservationsMapper observationsMapper;
    @Autowired
    private FHIRDiagnosticOrderMapper diagnosticOrderMapper;
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private EncounterService encounterService;
    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Override
    public boolean canHandle(Resource resource) {
        return resource instanceof DiagnosticReport;
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {
        DiagnosticReport diagnosticReport = (DiagnosticReport) resource;
        if (processedList.containsKey(diagnosticReport.getIdentifier().getValueSimple()))
            return;
        Concept concept = omrsHelper.findConcept(diagnosticReport.getName().getCoding());
        if (concept == null) {
            return;
        }
        Order order = getOrder(diagnosticReport, concept, feed, emrPatient, newEmrEncounter, processedList);
        if (order == null) {
            return;
        }
        Obs topLevelObs = buildObs(concept, order);

        Obs secondLevelObs = buildObs(concept, order);
        topLevelObs.addGroupMember(secondLevelObs);

        Obs resultObs = buildResultObs(feed, newEmrEncounter, processedList, diagnosticReport, concept);
        resultObs.setOrder(order);
        secondLevelObs.addGroupMember(resultObs);

        Obs notesObs = addNotes(diagnosticReport, order);
        secondLevelObs.addGroupMember(notesObs);

        if (order.getConcept().getConceptClass().getName().equals(MRS_CONCEPT_CLASS_LAB_SET)) {
            Obs panelObs = findObsByOrder(newEmrEncounter, order);
            if (panelObs == null) {
                panelObs = buildObs(order.getConcept(), order);
            }
            panelObs.addGroupMember(topLevelObs);
            newEmrEncounter.addObs(panelObs);
        }
        newEmrEncounter.addObs(topLevelObs);
        processedList.put(diagnosticReport.getIdentifier().getValueSimple(), Arrays.asList(topLevelObs.getUuid()));
    }

    private Order getOrder(DiagnosticReport diagnosticReport, Concept concept, AtomFeed feed, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {
        Order order = null;
        List<ResourceReference> requestDetail = diagnosticReport.getRequestDetail();
        for (ResourceReference reference : requestDetail) {
            String referenceString = reference.getReferenceSimple();
            if (isInternalReference(referenceString)) {
                order = getOrderFromInternalReference(concept, feed, emrPatient, newEmrEncounter, processedList, reference);
            } else {
                order = getOrderFromExternalReference(concept, referenceString);
            }
            if (order != null) break;
        }
        return order;
    }

    private boolean isInternalReference(String referenceString) {
        return !referenceString.startsWith("http://");
    }

    private Order getOrderFromExternalReference(Concept concept, String referenceString) {
        Pattern externalReferencePattern = Pattern.compile("http:\\/\\/(.*)\\/patients\\/(.*)\\/encounters\\/(.*)");
        Matcher matcher = externalReferencePattern.matcher(referenceString);
        if (matcher.matches()) {
            String shrEncounterId = matcher.group(3);
            if (!shrEncounterId.isEmpty()) {
                IdMapping orderEncounterIdMapping = idMappingsRepository.findByExternalId(shrEncounterId);
                Encounter orderEncounter = encounterService.getEncounterByUuid(orderEncounterIdMapping.getInternalId());
                Order order = findOrderFromEncounter(orderEncounter.getOrders(), concept);
                return order;
            }
        }
        return null;
    }

    private Order getOrderFromInternalReference(Concept concept, AtomFeed feed, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList, ResourceReference reference) {
        Order order;
        if (!processedList.containsKey(reference)) {
            Resource diagnosticOrder = findResourceByReference(feed, reference);
            diagnosticOrderMapper.map(feed, diagnosticOrder, emrPatient, newEmrEncounter, processedList);
        }
        order = findOrderFromEncounter(newEmrEncounter.getOrders(), concept);
        return order;
    }

    private Order findOrderFromEncounter(Set<Order> orders, Concept concept) {
        for (Order order : orders) {
            Concept orderConcept = order.getConcept();
            if (orderConcept.equals(concept)) {
                return order;
            } else if (orderConcept.getConceptClass().getName().equals(MRS_CONCEPT_CLASS_LAB_SET)) {
                for (Concept setMember : orderConcept.getSetMembers()) {
                    if (setMember.equals(concept)) {
                        return order;
                    }
                }
            }
        }
        return null;
    }

    private Obs addNotes(DiagnosticReport diagnosticReport, Order order) {
        String conclusion = diagnosticReport.getConclusionSimple();
        if (StringUtils.isNotBlank(conclusion)) {
            Concept labNotesConcept = conceptService.getConceptByName(MRS_CONCEPT_NAME_LAB_NOTES);
            Obs notesObs = buildObs(labNotesConcept, order);
            notesObs.setValueText(conclusion);
            return notesObs;
        }
        return null;
    }

    private Obs buildResultObs(AtomFeed feed, Encounter newEmrEncounter, Map<String, List<String>> processedList, DiagnosticReport diagnosticReport, Concept concept) {
        Observation observationResource = (Observation) findResourceByReference(feed, diagnosticReport.getResult().get(0));
        if (processedList.containsKey(observationResource.getIdentifier().getValueSimple())) {
            List<String> uuids = processedList.get(observationResource.getIdentifier().getValueSimple());
            for (String uuid : uuids) {
                Obs obs = findObsByUUid(newEmrEncounter, uuid);
                if (obs.getConcept().equals(concept)) {
                    return obs;
                }
            }
        }
        return observationsMapper.mapObs(feed, newEmrEncounter, observationResource, processedList);
    }

    private Obs findObsByUUid(Encounter newEmrEncounter, String obsUuid) {
        for (Obs obs : newEmrEncounter.getAllObs()) {
            if (obs.getUuid().equals(obsUuid))
                return obs;
        }
        return null;
    }

    private Obs findObsByOrder(Encounter encounter, Order order) {
        for (Obs obs : encounter.getObsAtTopLevel(false)) {
            if (obs.getOrder().equals(order) && obs.getConcept().equals(order.getConcept())) {
                return obs;
            }
        }
        return null;
    }

    private Obs buildObs(Concept concept, Order order) {
        Obs obs = new Obs();
        obs.setConcept(concept);
        obs.setOrder(order);
        return obs;
    }
}
