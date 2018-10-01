package com.paddypowerbetfair.springframework.keys.rollover;

import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.xml.encryption.ChainingEncryptedKeyResolver;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.encryption.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.context.SAMLMessageContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@link SAMLContextProviderImpl} which populates the {@link Decrypter} with the current encryption key
 * together with the rollover encryption key, if it is configured.
 */
@Slf4j
public class SAMLContextProviderKeysRolloverImpl extends SAMLContextProviderImpl {

    // Way to obtain encrypted key info from XML Encryption
    private static ChainingEncryptedKeyResolver encryptedKeyResolver = new ChainingEncryptedKeyResolver();

    static {
        encryptedKeyResolver.getResolverChain().add(new InlineEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new EncryptedElementTypeEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new SimpleRetrievalMethodEncryptedKeyResolver());
    }

    @Override
    protected void populateDecrypter(SAMLMessageContext samlContext) {
        // Locate current encryption key for this entity
        List<Credential> encryptionCredentials = new ArrayList<>();
        if (samlContext.getLocalExtendedMetadata().getEncryptionKey() != null) {
            encryptionCredentials.add(keyManager.getCredential(samlContext.getLocalExtendedMetadata().getEncryptionKey()));
        } else {
            encryptionCredentials.add(keyManager.getDefaultCredential());
        }

        // Locate rollover encryption key for this entity, if it exists
        if (samlContext.getLocalExtendedMetadata() instanceof ExtendedMetadataKeysRollover) {
            final ExtendedMetadataKeysRollover extendedMetadataKeysRollover = (ExtendedMetadataKeysRollover) samlContext.getLocalExtendedMetadata();

            if (extendedMetadataKeysRollover.getRolloverKeys() != null && extendedMetadataKeysRollover.getRolloverKeys().containsKey(UsageType.ENCRYPTION)) {
                final String rolloverKeyAlias = extendedMetadataKeysRollover.getRolloverKeys().get(UsageType.ENCRYPTION);

                log.debug("Populating the Decrypter with a rollover encryption key (alias = {})", rolloverKeyAlias);
                encryptionCredentials.add(keyManager.getCredential(rolloverKeyAlias));
            }

        }

        // Entity used for decrypting of encrypted XML parts
        // Extracts EncryptedKey from the encrypted XML using the encryptedKeyResolver and attempts to decrypt it
        // using private keys supplied by the resolver.
        KeyInfoCredentialResolver resolver = new StaticKeyInfoCredentialResolver(encryptionCredentials);

        Decrypter decrypter = new Decrypter(null, resolver, encryptedKeyResolver);
        decrypter.setRootInNewDocument(true);

        samlContext.setLocalDecrypter(decrypter);
    }
}
