package com.paddypowerbetfair.springframework.keys.rollover

import org.opensaml.xml.security.credential.Credential
import org.opensaml.xml.security.credential.StaticCredentialResolver
import org.opensaml.xml.security.credential.UsageType
import org.springframework.core.io.ClassPathResource
import org.springframework.security.saml.context.SAMLMessageContext
import org.springframework.security.saml.key.JKSKeyManager
import spock.lang.Specification

class SAMLContextProviderKeysRolloverImplTest extends Specification {

    def sAMLContextProvider = new SAMLContextProviderKeysRolloverImpl()


    def "setup"() {
        sAMLContextProvider.setKeyManager(new JKSKeyManager(
                new ClassPathResource("samlKeystoreRollover.jks"),
                "testKey",
                [
                        "current_key": "testKey",
                        "rollover_key": "testKey"
                ] as Map<String, String>,

                "current_key"
        ))
    }

    def "it should populate the Decrypter with just 1 Credential"() {
        given:
            def extendedMetadata = new ExtendedMetadataKeysRollover()
            def samlMessageContext = new SAMLMessageContext()
            samlMessageContext.setLocalExtendedMetadata(extendedMetadata)

        when:
            sAMLContextProvider.populateDecrypter(samlMessageContext)

        then:
            1 == ((samlMessageContext.getLocalDecrypter().getKEKResolver() as StaticCredentialResolver).resolve(null) as List<Credential>).size()
    }

    def "it should populate the Decrypter with 2 Credentials"() {
        given:
            def extendedMetadata = new ExtendedMetadataKeysRollover()
            def rolloverKeys = new HashMap<UsageType, String>()
            rolloverKeys.put(UsageType.ENCRYPTION, "rollover_key")
            rolloverKeys.put(UsageType.SIGNING, "rollover_key")
            extendedMetadata.setRolloverKeys(rolloverKeys)

            def samlMessageContext = new SAMLMessageContext()
            samlMessageContext.setLocalExtendedMetadata(extendedMetadata)

        when:
            sAMLContextProvider.populateDecrypter(samlMessageContext)

        then:
            2 == ((samlMessageContext.getLocalDecrypter().getKEKResolver() as StaticCredentialResolver).resolve(null) as List<Credential>).size()
    }
}
