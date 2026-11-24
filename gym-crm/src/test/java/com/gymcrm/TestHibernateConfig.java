package com.gymcrm;

import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import com.gymcrm.config.AppConfig;
import com.gymcrm.config.HibernateConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

//spring config for integration test DAO facade
//H2 in-memory is used so no changes are made to main database
//component scan of all com.gymcrm except config classes
//MeterRegistry is provided for GymMetrics,
@TestConfiguration
@EnableTransactionManagement
@ComponentScan(
    basePackages = "com.gymcrm",
    excludeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {AppConfig.class, HibernateConfig.class})
)
@PropertySource("classpath:application.properties")
public class TestHibernateConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean(destroyMethod = "close")
    public DataSource dataSource(
            @Value("${spring.datasource.url}")               String url,
            @Value("${spring.datasource.username}")          String username,
            @Value("${spring.datasource.password}")          String password,
            @Value("${spring.datasource.driver-class-name}") String driver) {

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driver);
        ds.setMaximumPoolSize(5);
        ds.setPoolName("TestPool");
        return ds;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(
            DataSource dataSource,
            @Value("${hibernate.dialect}")      String dialect,
            @Value("${hibernate.hbm2ddl.auto}") String ddlAuto,
            @Value("${hibernate.show_sql:false}") String showSql) {

        LocalSessionFactoryBean factory = new LocalSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.gymcrm.model");

        Properties props = new Properties();
        props.setProperty("hibernate.dialect", dialect);
        props.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
        props.setProperty("hibernate.show_sql", showSql);
        props.setProperty("hibernate.current_session_context_class",
                "org.springframework.orm.hibernate5.SpringSessionContext");
        factory.setHibernateProperties(props);
        return factory;
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sf) {
        return new HibernateTransactionManager(sf);
    }

    //without this, dbunit auto-wraps the plain dataSource bean with no schema qualifier
    //and metadata scan on H2 sees the USERS table as ambiguous
    @Bean
    public DatabaseDataSourceConnectionFactoryBean dbUnitDatabaseConnection(DataSource dataSource) {
        DatabaseDataSourceConnectionFactoryBean factory = new DatabaseDataSourceConnectionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setSchema("PUBLIC");
        return factory;
    }
}
