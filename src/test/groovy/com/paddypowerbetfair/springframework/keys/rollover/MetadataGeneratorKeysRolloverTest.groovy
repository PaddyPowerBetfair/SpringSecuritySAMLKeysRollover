package com.paddypowerbetfair.springframework.keys.rollover

import org.opensaml.saml2.metadata.AssertionConsumerService
import org.opensaml.saml2.metadata.KeyDescriptor
import org.opensaml.saml2.metadata.SPSSODescriptor
import org.opensaml.saml2.metadata.SingleLogoutService
import org.opensaml.saml2.metadata.impl.AssertionConsumerServiceBuilder
import org.opensaml.saml2.metadata.impl.KeyDescriptorBuilder
import org.opensaml.saml2.metadata.impl.SPSSODescriptorBuilder
import org.opensaml.saml2.metadata.impl.SingleLogoutServiceBuilder
import org.opensaml.xml.Configuration
import org.opensaml.xml.security.BasicSecurityConfiguration
import org.opensaml.xml.security.credential.UsageType
import org.opensaml.xml.security.keyinfo.NamedKeyInfoGeneratorManager
import org.opensaml.xml.security.x509.X509KeyInfoGeneratorFactory
import org.opensaml.xml.signature.KeyInfo
import org.opensaml.xml.signature.X509Data
import org.opensaml.xml.signature.impl.KeyInfoBuilder
import org.opensaml.xml.signature.impl.X509DataBuilder
import org.springframework.core.io.ClassPathResource
import org.springframework.security.saml.key.JKSKeyManager
import spock.lang.Specification

class MetadataGeneratorKeysRolloverTest extends Specification {

    def metadataGenerator = new MetadataGeneratorKeysRollover()

    def "setup"() {
        Configuration.getBuilderFactory().registerBuilder(SPSSODescriptor.DEFAULT_ELEMENT_NAME, new SPSSODescriptorBuilder())
        Configuration.getBuilderFactory().registerBuilder(AssertionConsumerService.DEFAULT_ELEMENT_NAME, new AssertionConsumerServiceBuilder())
        Configuration.getBuilderFactory().registerBuilder(SingleLogoutService.DEFAULT_ELEMENT_NAME, new SingleLogoutServiceBuilder())
        Configuration.getBuilderFactory().registerBuilder(KeyInfo.DEFAULT_ELEMENT_NAME, new KeyInfoBuilder())
        Configuration.getBuilderFactory().registerBuilder(X509Data.DEFAULT_ELEMENT_NAME, new X509DataBuilder())
        Configuration.getBuilderFactory().registerBuilder(KeyDescriptor.DEFAULT_ELEMENT_NAME, new KeyDescriptorBuilder())

        def securityConfig = new BasicSecurityConfiguration()
        def keyInfoManager = new NamedKeyInfoGeneratorManager()
        keyInfoManager.registerFactory("MetadataKeyInfoGenerator", new X509KeyInfoGeneratorFactory())
        securityConfig.setKeyInfoGeneratorManager(keyInfoManager)
        Configuration.setGlobalSecurityConfiguration(securityConfig)

        metadataGenerator.setKeyManager(new JKSKeyManager(
                new ClassPathResource("samlKeystoreRollover.jks"),
                "testKey",
                [
                    "current_key": "testKey",
                    "rollover_key": "testKey"
                ] as Map<String, String>,
                "current_key"
        ))
    }


    def "it must create 2 KeyDescriptors if no rollover config is set"() {
        given:
            def entityBaseURL = "http://entity.base.url"

        when:
            def spSSODescriptor = metadataGenerator.buildSPSSODescriptor(entityBaseURL, null, true, true, Collections.emptyList())

        then:
            2 == spSSODescriptor.getKeyDescriptors().size()
    }


    def "it must create 4 KeyDescriptors if the rollover config is set"() {
        given:
            def entityBaseURL = "http://entity.base.url"
            def extendedMetadata = new ExtendedMetadataKeysRollover()
            def rolloverKeys = new HashMap<UsageType, String>()
            rolloverKeys.put(UsageType.ENCRYPTION, "rollover_key")
            rolloverKeys.put(UsageType.SIGNING, "rollover_key")
            extendedMetadata.setRolloverKeys(rolloverKeys)

            metadataGenerator.setExtendedMetadata(extendedMetadata)

        when:
            def spSSODescriptor = metadataGenerator.buildSPSSODescriptor(entityBaseURL, null, true, true, Collections.emptyList())

        then:
            4 == spSSODescriptor.getKeyDescriptors().size()
    }
}
