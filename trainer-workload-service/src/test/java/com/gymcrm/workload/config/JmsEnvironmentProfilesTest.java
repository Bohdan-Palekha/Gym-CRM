package com.gymcrm.workload.config;

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

@DisplayName("ActiveMQ broker + consumer concurrency configuration per environment profile")
class JmsEnvironmentProfilesTest {

    @ParameterizedTest(name = "application-{0}.properties defines a broker URL and consumer concurrency")
    @ValueSource(strings = {"local", "dev", "stg", "prod"})
    @DisplayName("every profile defines a broker URL and a consumer concurrency range")
    void everyProfile_definesBrokerUrlAndConcurrency(String profile) throws IOException {
        Properties properties = loadProfile(profile);

        String brokerUrl = properties.getProperty("spring.activemq.broker-url");
        assertNotNull(brokerUrl, "application-" + profile + ".properties must define spring.activemq.broker-url");
        assertTrue(brokerUrl.contains("ACTIVEMQ_BROKER_URL"),
                "broker url should be overridable via the ACTIVEMQ_BROKER_URL environment variable");

        String concurrency = properties.getProperty("trainer-workload.jms.listener.concurrency");
        assertNotNull(concurrency, profile + " should tune consumer concurrency explicitly");
        assertTrue(concurrency.matches("\\d+-\\d+"), "expected a min-max concurrency range like '2-4'");
    }

    @ParameterizedTest(name = "{0} profile defaults to a locally reachable broker")
    @ValueSource(strings = {"local", "dev"})
    @DisplayName("local/dev profiles have a developer-friendly default broker URL")
    void localAndDevProfiles_haveConvenientDefault(String profile) throws IOException {
        Properties properties = loadProfile(profile);

        assertTrue(properties.getProperty("spring.activemq.broker-url").contains(":tcp://localhost:61616"),
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

    @Test
    @DisplayName("consumer concurrency scales up from local to prod")
    void concurrency_scalesUpTowardsProduction() throws IOException {
        int local = maxConcurrency(loadProfile("local"));
        int dev = maxConcurrency(loadProfile("dev"));
        int stg = maxConcurrency(loadProfile("stg"));
        int prod = maxConcurrency(loadProfile("prod"));

        assertTrue(local <= dev && dev <= stg && stg <= prod,
                "expected non-decreasing consumer capacity from local through prod, got: "
                        + local + ", " + dev + ", " + stg + ", " + prod);
    }

    private int maxConcurrency(Properties properties) {
        String range = properties.getProperty("trainer-workload.jms.listener.concurrency");
        return Integer.parseInt(range.split("-")[1]);
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
