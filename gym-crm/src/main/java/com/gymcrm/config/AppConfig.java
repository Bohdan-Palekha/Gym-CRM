package com.gymcrm.config;

import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

//exclude test-only configs, because of collisions with HibernateConfig's transactionManager beans,  @SpringBootApplication seems fine
//regex because cannot reference src/test/java
@Configuration
@ComponentScan(
        basePackages = "com.gymcrm",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.gymcrm\\.TestHibernateConfig"))
@PropertySource("classpath:application.properties")
@Import(HibernateConfig.class)
@EnableAspectJAutoProxy
public class AppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    // Trainer and trainee are entities, managed by hibernate
}

