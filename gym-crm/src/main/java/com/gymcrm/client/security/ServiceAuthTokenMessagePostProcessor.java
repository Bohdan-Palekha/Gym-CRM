package com.gymcrm.client.security;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.springframework.jms.core.MessagePostProcessor;

//JMS counterpart of the removed ServiceBearerTokenInterceptor
//call to trainer-workload-service is now a message(was http request)
//thus the bearer token travels as a JMS message property instead of a header
//per message mint makes this stateless, no expired token is sent
public class ServiceAuthTokenMessagePostProcessor implements MessagePostProcessor {

    //must match the property name TrainerWorkloadUpdateListener reads on the trainer-workload-service side
    public static final String SERVICE_AUTH_TOKEN_PROPERTY = "serviceAuthToken";

    private final InterServiceTokenProvider tokenProvider;

    public ServiceAuthTokenMessagePostProcessor(InterServiceTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Message postProcessMessage(Message message) throws JMSException {
        message.setStringProperty(SERVICE_AUTH_TOKEN_PROPERTY, tokenProvider.generateToken());
        return message;
    }
}
