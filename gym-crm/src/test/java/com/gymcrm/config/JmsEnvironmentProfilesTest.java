package com.gymcrm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ActiveMQ broker configuration per environment profile")
class JmsEnvironmentProfilesTest {

    @ParameterizedTest(name = "application-{0}.properties defines spring.activemq.broker-url")
    @ValueSource(strings = {"local", "dev", "stg", "prod"})
    @DisplayName("every profile defines a broker URL")
    void everyProfile_definesBrokerUrl(String profile) throws IOException {
        Properties properties = loadProfile(profile);

        String brokerUrl = properties.getProperty("spring.activemq.broker-url");
        assertNotNull(brokerUrl, "application-" + profile + ".properties must define spring.activemq.broker-url");
        assertTrue(brokerUrl.contains("ACTIVEMQ_BROKER_URL"),
                "broker url should be overridable via the ACTIVEMQ_BROKER_URL environment variable");
    }

    @ParameterizedTest(name = "{0} profile defaults to a locally reachable broker")
    @ValueSource(strings = {"local", "dev"})
    @DisplayName("local/dev profiles have a developer-friendly default broker URL")
    void localAndDevProfiles_haveConvenientDefault(String profile) throws IOException {
        Properties properties = loadProfile(profile);

        String brokerUrl = properties.getProperty("spring.activemq.broker-url");
        assertTrue(brokerUrl.contains(":tcp://localhost:61616"),
                profile + " should default to a locally reachable broker for developer convenience");
    }

    @ParameterizedTest(name = "{0} profile requires an explicit broker URL")
    @ValueSource(strings = {"stg", "prod"})
    @DisplayName("stg/prod profiles have no insecure/implicit broker default")
    void stgAndProdProfiles_requireExplicitBrokerUrl(String profile) throws IOException {
        Properties properties = loadProfile(profile);

        assertEquals("${ACTIVEMQ_BROKER_URL}", properties.getProperty("spring.activemq.broker-url"),
                profile + " must require ACTIVEMQ_BROKER_URL to be set explicitly, no silent default");
    }

    private Properties loadProfile(String profile) throws IOException {
        String fileName = "application-" + profile + ".properties";
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
            assertNotNull(in, fileName + " should exist on the classpath");
            properties.load(in);
        }
        return properties;
    }
}
