package org.openmrs.module.fhir.utils;

import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Concept;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FHIRFeedHelper {

    public static CodeableConcept getFHIRCodeableConcept(String code, String system, String display) {
        CodeableConcept codeableConcept = new CodeableConcept();
        addFHIRCoding(codeableConcept, code, system, display);
        return codeableConcept;
    }

    public static void addFHIRCoding(CodeableConcept codeableConcept, String code, String system, String display) {
        Coding coding = codeableConcept.addCoding();
        coding.setCodeSimple(code);
        coding.setSystemSimple(system);
        coding.setDisplaySimple(display);
    }

    public static CodeableConcept addReferenceCodes(Concept concept, IdMappingsRepository idMappingsRepository) {
        CodeableConcept codeableConcept = new CodeableConcept();
        Collection<org.openmrs.ConceptMap> conceptMappings = concept.getConceptMappings();
        for (org.openmrs.ConceptMap mapping : conceptMappings) {
            ConceptReferenceTerm conceptReferenceTerm = mapping.getConceptReferenceTerm();
            final IdMapping idMapping = idMappingsRepository.findByInternalId(conceptReferenceTerm.getUuid());
            if(idMapping == null) {
                continue;
            }
            addFHIRCoding(codeableConcept, conceptReferenceTerm.getCode(), idMapping.getUri(), concept.getName().getName());
        }
        IdMapping idMapping = idMappingsRepository.findByInternalId(concept.getUuid());
        if(idMapping != null) {
            addFHIRCoding(codeableConcept, idMapping.getExternalId(), idMapping.getUri(), concept.getName().getName());
        }
        return codeableConcept;
    }

    public static String getValueSetCode(Concept concept) {
        for (org.openmrs.ConceptMap mapping : concept.getConceptMappings()) {
            if (mapping.getConceptMapType().getUuid().equals(ConceptMapType.SAME_AS_MAP_TYPE_UUID)) {
                return mapping.getConceptReferenceTerm().getCode();
            }
        }
        for (ConceptName conceptName : concept.getShortNames()) {
            return conceptName.getName();
        }
        return concept.getName().getName();
    }

    public static Composition getComposition(AtomFeed bundle) {
        Resource resource = identifyResource(bundle.getEntryList(), ResourceType.Composition);
        return resource != null ? (Composition) resource : null;
    }

    public static Resource identifyResource(List<AtomEntry<? extends Resource>> encounterBundleEntryList, ResourceType resourceType) {
        for (AtomEntry<? extends org.hl7.fhir.instance.model.Resource> atomEntry : encounterBundleEntryList) {
            if (atomEntry.getResource().getResourceType().equals(resourceType)) {
                return atomEntry.getResource();
            }
        }
        return null;
    }

    public static List<Resource> identifyResources(List<AtomEntry<? extends Resource>> encounterBundleEntryList, ResourceType resourceType) {
        List<Resource> resources = new ArrayList<>();
        for (AtomEntry<? extends org.hl7.fhir.instance.model.Resource> atomEntry : encounterBundleEntryList) {
            if (atomEntry.getResource().getResourceType().equals(resourceType)) {
                resources.add(atomEntry.getResource());
            }
        }
        return resources;
    }

    public static Encounter getEncounter(AtomFeed bundle) {
        Resource resource = identifyResource(bundle.getEntryList(), ResourceType.Encounter);
        return resource != null ? (Encounter) resource : null;
    }

    public static List<Condition> getConditions(AtomFeed bundle) {
        List<Condition> conditions = new ArrayList<Condition>();
        List<AtomEntry<? extends Resource>> entryList = bundle.getEntryList();
        for (AtomEntry<? extends org.hl7.fhir.instance.model.Resource> atomEntry : entryList) {
            Resource resource = atomEntry.getResource();
            if (resource.getResourceType().equals(ResourceType.Condition)) {
                conditions.add((Condition) resource);
            }
        }
        return conditions;
    }

    public static Resource findResourceByReference(AtomFeed bundle, ResourceReference reference) {
        for (AtomEntry<? extends Resource> atomEntry : bundle.getEntryList()) {
            if (StringUtils.equals(atomEntry.getId(), reference.getReferenceSimple())) {
                return atomEntry.getResource();
            }
        }
        return null;
    }
}
