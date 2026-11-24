package com.gymcrm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Environment profile properties (task 5)")
class EnvironmentProfilesTest {

    private static Properties load(String resourceName) throws IOException {
        Properties props = new Properties();
        try (InputStream in = EnvironmentProfilesTest.class.getClassLoader()
                .getResourceAsStream(resourceName)) {
            assertNotNull(in, resourceName + " must be on the classpath");
            props.load(in);
        }
        return props;
    }

    @Test
    @DisplayName("local profile uses a zero-setup embedded H2 database")
    void local_usesEmbeddedH2() throws IOException {
        Properties props = load("application-local.properties");
        assertEquals("org.h2.Driver", props.getProperty("spring.datasource.driver-class-name"));
        assertTrue(props.getProperty("spring.datasource.url").startsWith("jdbc:h2:mem:"));
        assertEquals("create-drop", props.getProperty("hibernate.hbm2ddl.auto"));
    }

    @Test
    @DisplayName("dev profile targets MySQL with schema auto-update enabled")
    void dev_usesMySqlWithSchemaUpdate() throws IOException {
        Properties props = load("application-dev.properties");
        assertEquals("com.mysql.cj.jdbc.Driver", props.getProperty("spring.datasource.driver-class-name"));
        assertEquals("update", props.getProperty("hibernate.hbm2ddl.auto"));
    }

    @Test
    @DisplayName("stg and prod validate the schema instead of auto-altering a shared database")
    void stgAndProd_validateSchemaInsteadOfUpdating() throws IOException {
        Properties stg = load("application-stg.properties");
        Properties prod = load("application-prod.properties");
        assertEquals("validate", stg.getProperty("hibernate.hbm2ddl.auto"));
        assertEquals("validate", prod.getProperty("hibernate.hbm2ddl.auto"));
    }

    @Test
    @DisplayName("stg and prod externalize DB credentials rather than hardcoding them")
    void stgAndProd_externalizeCredentials() throws IOException {
        Properties stg = load("application-stg.properties");
        Properties prod = load("application-prod.properties");
        assertTrue(stg.getProperty("spring.datasource.password").startsWith("${DB_PASSWORD"));
        assertTrue(prod.getProperty("spring.datasource.password").startsWith("${DB_PASSWORD"));
        assertTrue(prod.getProperty("spring.datasource.url").startsWith("${DB_URL"));
    }

    @Test
    @DisplayName("security task 8 — stg and prod require an externally supplied JWT secret, with no insecure fallback")
    void stgAndProd_requireExternalJwtSecretWithNoDefault() throws IOException {
        Properties stg = load("application-stg.properties");
        Properties prod = load("application-prod.properties");
        assertEquals("${JWT_SECRET}", stg.getProperty("security.jwt.secret"),
                "must have no default value, so a missing env var fails startup rather than signing with a known secret");
        assertEquals("${JWT_SECRET}", prod.getProperty("security.jwt.secret"));
    }

    @Test
    @DisplayName("local and dev provide a JWT secret out of the box, still overridable via JWT_SECRET")
    void localAndDev_provideDevFriendlyJwtSecretDefault() throws IOException {
        for (String profile : new String[]{"local", "dev"}) {
            Properties props = load("application-" + profile + ".properties");
            String secret = props.getProperty("security.jwt.secret");
            assertNotNull(secret, profile + " must declare a JWT secret");
            assertTrue(secret.startsWith("${JWT_SECRET:"), profile + " default must still be overridable via JWT_SECRET");
        }
    }

    @Test
    @DisplayName("every environment declares its own datasource url and hibernate dialect")
    void everyProfile_declaresDatasourceAndDialect() throws IOException {
        for (String profile : new String[]{"local", "dev", "stg", "prod"}) {
            Properties props = load("application-" + profile + ".properties");
            assertNotNull(props.getProperty("spring.datasource.url"),
                    profile + " must declare a datasource url");
            assertNotNull(props.getProperty("hibernate.dialect"),
                    profile + " must declare a hibernate dialect");
        }
    }
}
