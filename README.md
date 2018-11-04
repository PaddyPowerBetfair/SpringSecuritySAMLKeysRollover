# Spring Security SAML Keys Rollover

[![Maven Central](https://img.shields.io/maven-central/v/com.paddypowerbetfair/spring-security-saml-keys-rollover.svg?style=plastic)](https://search.maven.org/artifact/com.paddypowerbetfair/spring-security-saml-keys-rollover/)


An extension of [Spring Security SAML][SpringSecuritySAML] Library 1.0.x to add support for keys rollover.

SAML 2.0 uses asymmetric cryptography to sign and encrypt messages that are sent between Service Providers (SPs) and Identity Providers (IdPs).
Each SAML entity has, at least a key pair along with an X.509 certificate to be are distributed using the standard SAML XML metadata
When the certificates are about to expire, or due to security reasons, a key rollover must occur so that there's no service interruption.

The general process of rolling over a key on a Service Provider without any service interruption is as follows:

1. Create a new key pair for signing and/or encryption together with the respective X.509 certificate
2. Configure your SP to support the new key pair
    1. Add a new KeyDescriptor to your SAML metadata
    2. Support decrypting SAML messages using your new key
3. Send your metadata (or just the X.509 certificate) to the IdP(s). They must:
    1. Switch the encryption certificate to the new one
    2. Trust in your new signing certificate, without stop trusting in the old one
4. Wait for the IdP(s) to update its configurations
   1. Do not start to use the new key for signing your messages until the IdP(s) confirm they are supporting your new certificate
5. Configure your SP to start using the new key for signing messages
    1. The old keys may be completely removed
    2. The IdP(s) can now untrust your old signing certificate


## Supporting local key rollover in your project for both signing and encryption - a step-by-step guide

### 1. Add the maven dependency

This dependency extends some [Spring Security SAML][SpringSecuritySAML] classes to support the Key Rollover feature.

```xml
<dependency>
    <groupId>com.paddypowerbetfair</groupId>
    <artifactId>spring-security-saml-keys-rollover</artifactId>
    <version>1.0.2</version>
</dependency>
```

### 2. Generate a new key pair and respective self-signed certificate

Please make sure if you want to use self-signed certificates for your deployment.
If you don't, you should import a CA-signed certificate to your KeyStore instead.

```bash
keytool -genkey -alias <new_key_alias> -validity <number_of_day> -keyalg RSA -keystore <keystore_file>.jks
```

### 3. Update your `keyManager` configuration to include the new private key password

```xml
<bean id="keyManager" class="org.springframework.security.saml.key.JKSKeyManager">
    <constructor-arg value="classpath:<keystore_file>.jks"/>
    <constructor-arg type="java.lang.String" value="keystore_pass"/>
    <constructor-arg>
        <map>
            <entry key="old_key_alias" value="old_key_pass"/>
            <entry key="new_key_alias" value="new_key_pass"/>
        </map>
    </constructor-arg>
    <constructor-arg type="java.lang.String" value="old_key_alias"/>
</bean>
```

### 4. Update your `metadataGeneratorFilter` configuration to use a `MetadataGeneratorKeysRollover` together with an `extendedMetatada` attribute using a `ExtendedMetadataKeysRollover` bean

By updating your `metadataGeneratorFilter` configuration, your XML metadata will include both (current and new) the certificates so that IdP(s) can update their configurations.

```xml
<bean id="metadataGeneratorFilter" class="org.springframework.security.saml.metadata.MetadataGeneratorFilter">
    <constructor-arg>
        <bean class="com.paddypowerbetfair.springframework.keys.rollover.MetadataGeneratorKeysRollover">
            <!-- Your usual content here -->

            <property name="extendedMetadata">
                <bean class="com.paddypowerbetfair.springframework.keys.rollover.ExtendedMetadataKeysRollover">
                    <property name="rolloverKeys">
                        <map>
                            <entry key="ENCRYPTION" value="new_key_alias"/>
                            <entry key="SIGNING" value="new_key_alias"/>
                        </map>
                    </property>
                </bean>
            </property>
        </bean>
    </constructor-arg>
</bean>
``` 

### 5. Update your SAML `contextProvider` bean to use the `SAMLContextProviderKeysRolloverImpl` class

By using the `SAMLContextProviderKeysRolloverImpl` the SAML message decrypter will be able to decrypt messages using any of your encryption private keys.
With this, your SP will be able to decrypt messages coming from IdPs who have not yet updated their configurations along with the messages from the IdPs who have updated.

```xml
<bean id="contextProvider" class="com.paddypowerbetfair.springframework.keys.rollover.SAMLContextProviderKeysRolloverImpl"/>
```

## How can I contribute?
Please see [CONTRIBUTING.md](CONTRIBUTING.md).

## What licence is this released under?
This is released under a modified version of the BSD licence.
Please see [LICENSE](LICENSE).

[SpringSecuritySAML]: https://projects.spring.io/spring-security-saml/