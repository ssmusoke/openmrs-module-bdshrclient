package org.openmrs.module.shrclient.service;

import org.openmrs.Order;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.api.OrderService;
import org.openmrs.api.PersonService;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.serialization.SerializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service("hieEmrPatientMergeService")
public class EMRPatientMergeService {


    private OrderService orderService;
    private EMRPatientService emrPatientService;
    private PersonService personService;
    private IdMappingRepository idMappingRepository;

    @Autowired
    public EMRPatientMergeService(PersonService personService,
                                  OrderService orderService,
                                  EMRPatientService emrPatientService, IdMappingRepository idMappingRepository) {
        this.orderService = orderService;
        this.personService = personService;
        this.emrPatientService = emrPatientService;
        this.idMappingRepository = idMappingRepository;
    }


    public void mergePatients(String toBeRetainedHealthId, String toBeRetiredHealthId) throws SerializationException {
        org.openmrs.Patient toBeRetainedPatient = emrPatientService.getEMRPatientByHealthId(toBeRetainedHealthId);
        org.openmrs.Patient toBeRetiredPatient = emrPatientService.getEMRPatientByHealthId(toBeRetiredHealthId);

        voidOrRetirePatientData(toBeRetiredPatient, "Merged with " + toBeRetiredHealthId);
        List<Order> voidedOrdersList = voidAllUnvoidedOrders(toBeRetiredPatient);

        emrPatientService.mergePatients(toBeRetainedPatient, toBeRetiredPatient);
        idMappingRepository.replaceHealthId(toBeRetiredHealthId, toBeRetainedHealthId);
        unVoidRequiredOrders(voidedOrdersList);
    }

    private List<Order> voidAllUnvoidedOrders(org.openmrs.Patient toBeRetiredPatient) {
        List<Order> voidedOrders = new ArrayList<>();
        List<Order> orders = orderService.getAllOrdersByPatient(toBeRetiredPatient);
        for(Order order : orders) {
            if(!order.isVoided()) {
                order.setVoided(true);
                voidedOrders.add(order);

            }
        }
        return voidedOrders;
    }

    private void unVoidRequiredOrders(List<Order> voidedOrdersList) {
        for(Order order : voidedOrdersList) {
            order.setVoided(false);
        }

    }

    private void voidOrRetirePatientData(org.openmrs.Patient toBeRetiredPatient, String voidReason) {
        voidAddress(toBeRetiredPatient, voidReason);
        removeAttributes(toBeRetiredPatient);
        voidIdentifiers(toBeRetiredPatient);
    }

    private void voidIdentifiers(org.openmrs.Patient toBeRetiredPatient) {
        Set<PatientIdentifier> identifiers = toBeRetiredPatient.getIdentifiers();
        for(PatientIdentifier identifier : identifiers) {
            identifier.setVoided(true);
        }
    }

    private void removeAttributes(org.openmrs.Patient toBeRetiredPatient) {
        Set<PersonAttribute> attributes = toBeRetiredPatient.getAttributes();
        for(PersonAttribute attribute : new HashSet<>(attributes)) {
            toBeRetiredPatient.removeAttribute(attribute);
        }
    }

    private void voidAddress(org.openmrs.Patient toBeRetiredPatient, String voidReason) {
        Set<PersonAddress> addresses = toBeRetiredPatient.getAddresses();
        for(PersonAddress address : addresses) {
            personService.voidPersonAddress(address, voidReason);
        }
    }
}

