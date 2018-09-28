package com.paddypowerbetfair.springframework.keys.rollover;

import lombok.Getter;
import lombok.Setter;
import org.opensaml.xml.security.credential.UsageType;
import org.springframework.security.saml.metadata.ExtendedMetadata;

import java.util.Map;

/**
 * Extension of {@link ExtendedMetadata} to receive a Map of keys used to perform a rollover of local certificates.
 */
@Getter
@Setter
public class ExtendedMetadataKeysRollover extends ExtendedMetadata {

    /**
     * Similar to {@link #signingKey} and {@link #encryptionKey} but used to set new keys to perform a local certificate
     * rollover.
     */
    private Map<UsageType, String> rolloverKeys;
}
