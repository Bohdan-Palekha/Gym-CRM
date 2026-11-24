package com.gymcrm.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//fallback reg of TimedAspect required for @Timed to be picked up by AOP, if bean is absent
//spring boot 3.2 already auto-configures TimedAspect MetricsAspectsAutoConfiguration when there is MeterRegistry
//thus missing bean conditional
@Configuration
public class MetricsConfig {

    @Bean
    @ConditionalOnMissingBean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

