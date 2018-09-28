package com.paddypowerbetfair.springframework.keys.rollover;

import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.springframework.security.saml.metadata.MetadataGenerator;

import java.util.Collection;

/**
 * Extension of {@link MetadataGenerator} but with support to add rollover certificates to the generated XML metadata.
 */
@Slf4j
public class MetadataGeneratorKeysRollover extends MetadataGenerator {

    /**
     * Extends the {@link MetadataGenerator#buildSPSSODescriptor(String, String, boolean, boolean, Collection)} method
     * to add the rollover certificates, if they are configured.
     */
    @Override
    protected SPSSODescriptor buildSPSSODescriptor(String entityBaseURL, String entityAlias, boolean requestSigned, boolean wantAssertionSigned, Collection<String> includedNameID) {
        final SPSSODescriptor spDescriptor = super.buildSPSSODescriptor(entityBaseURL, entityAlias, requestSigned, wantAssertionSigned, includedNameID);

        if (getExtendedMetadata() instanceof ExtendedMetadataKeysRollover) {
            final ExtendedMetadataKeysRollover extendedMetadataKeysRollover = (ExtendedMetadataKeysRollover) getExtendedMetadata();

            if (extendedMetadataKeysRollover.getRolloverKeys() != null) {
                extendedMetadataKeysRollover.getRolloverKeys().forEach(((usageType, keyAlias) -> {
                    log.info("Adding a new key (alias = {}) for {} to the metadata", keyAlias, usageType);
                    spDescriptor.getKeyDescriptors().add(getKeyDescriptor(usageType, getServerKeyInfo(keyAlias)));
                }));
            }
        }

        return spDescriptor;
    }
}
