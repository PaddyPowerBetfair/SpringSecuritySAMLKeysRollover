# Spring Security SAML Keys Rollover

An extension of Spring Security SAML Library 1.0.x to add support for keys rollover.


## Supporting local key rollover in your project for both signin and encryption

1. Add the following maven dependency
```xml
<dependency>
    <groupId>com.paddypowerbetfair.springframework</groupId>
    <artifactId>spring-security-saml-keys-rollover</artifactId>
    <version>1.0.1</version>
</dependency>
```

2. Generate a new key pair and respective self-signed certificate
```bash
keytool -genkey -alias <new_key_alias> -validity <number_of_day> -keyalg RSA -keystore <keystore_file>.jks
```

3. Update your `keyManager` configuration to include the new private key password
```xml
<bean id="keyManager" class="org.springframework.security.saml.key.JKSKeyManager">
    <constructor-arg value="classpath:<keystore_file>.jks"/>
    <constructor-arg type="java.lang.String" value="nag1os"/>
    <constructor-arg>
        <map>
            <entry key="old_key_alias" value="old_key_pass"/>
            <entry key="new_key_alias" value="new_key_pass"/>
        </map>
    </constructor-arg>
    <constructor-arg type="java.lang.String" value="old_key_alias"/>
</bean>
```

4. Update your `metadataGeneratorFilter` configuration to use a `MetadataGeneratorKeysRollover` together with an `extendedMetatada` attribure using a `ExtendedMetadataKeysRollover` bean
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

5. Update your SAML `contextProvider` bean to use the `SAMLContextProviderKeysRolloverImpl` class
```xml
<bean id="contextProvider" class="com.paddypowerbetfair.springframework.keys.rollover.SAMLContextProviderKeysRolloverImpl"/>
```

## Releasing
```bash
mvn deploy -Dmaven.wagon.http.ssl.insecure=true
```