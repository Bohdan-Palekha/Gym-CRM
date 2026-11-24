package com.gymcrm.workload.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

//TrainerWorkloadRequest is already validated on the way (TrainerWorkloadUpdateListener)
//registers same jakarta.validation constraints declared on TrainerWorkloadDocument + nested year/month
//documents cannot reach MongoDB with blank username, null status, out of range year/month
@Configuration
public class MongoConfig {

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener(LocalValidatorFactoryBean validatorFactory) {
        return new ValidatingMongoEventListener(validatorFactory);
    }
}
