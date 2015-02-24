package org.openmrs.module.shrclient.mapper;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.ProviderAttributeType;
import org.openmrs.api.ProviderService;
import org.openmrs.module.shrclient.model.ProviderEntry;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;
import static org.openmrs.module.shrclient.mapper.ProviderMapper.RETIRE_REASON;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class ProviderMapperTest extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ProviderService providerService;
    @Autowired
    private ProviderMapper providerMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/providerDS.xml");
    }

    @Test
    public void shouldCreateNewProvider() throws Exception {
        String identifier = "1022";
        ProviderEntry providerEntry = getProviderEntry(identifier, "1");
        assertNull(providerService.getProviderByIdentifier(identifier));
        providerMapper.createOrUpdate(providerEntry);

        Provider provider = providerService.getProviderByIdentifier(identifier);
        assertEquals("Provider Name", provider.getName());
        assertFalse(provider.isRetired());
        assertEquals(1, provider.getAttributes().size());
        ProviderAttributeType providerAttributeType = providerService.getProviderAttributeType(1);
        ProviderAttribute organization = provider.getActiveAttributes(providerAttributeType).get(0);
        assertEquals("2222", organization.getValue());
    }

    @Test
    public void shouldUpdateProviderName() throws Exception {
        String identifier = "1023";
        ProviderEntry providerEntry = getProviderEntry(identifier, "1");
        Provider existingProvider = providerService.getProvider(22);
        assertEquals(identifier, existingProvider.getIdentifier());
        assertEquals("test provider", existingProvider.getName());

        providerMapper.createOrUpdate(providerEntry);

        Provider provider = providerService.getProvider(22);
        assertEquals(identifier, provider.getIdentifier());
        assertEquals("Provider Name", provider.getName());
    }

    @Test
    public void shouldUpdateProviderOrganization() throws Exception {
        ProviderAttributeType organizationAttributeType = providerService.getProviderAttributeType(1);
        ProviderEntry providerEntry = getProviderEntry("1024", "1");
        Provider existingProvider = providerService.getProvider(23);
        ProviderAttribute providerAttribute = existingProvider.getActiveAttributes(organizationAttributeType).get(0);
        assertEquals("1022", providerAttribute.getValue());

        providerMapper.createOrUpdate(providerEntry);

        Provider provider = providerService.getProvider(23);
        ProviderAttribute organization = provider.getActiveAttributes(organizationAttributeType).get(0);
        assertEquals("2222", organization.getValue());
    }

    @Test
    public void shouldRetireProviderIfNotActive() throws Exception {
        String identifier = "1023";
        ProviderEntry providerEntry = getProviderEntry(identifier, "0");
        Provider existingProvider = providerService.getProvider(22);
        assertEquals(identifier, existingProvider.getIdentifier());
        assertFalse(existingProvider.isRetired());

        providerMapper.createOrUpdate(providerEntry);

        Provider provider = providerService.getProvider(22);
        assertEquals(identifier, provider.getIdentifier());
        assertTrue(provider.isRetired());
        assertEquals(RETIRE_REASON, provider.getRetireReason());
    }

    private ProviderEntry getProviderEntry(String identifier, String active) {
        ProviderEntry providerEntry = new ProviderEntry();
        providerEntry.setId(identifier);
        providerEntry.setName("Provider Name");
        providerEntry.setActive(active);
        ProviderEntry.Organization organization = providerEntry.new Organization();
        organization.setReference("http://something/2222.json");
        providerEntry.setOrganization(organization);
        return providerEntry;
    }
}