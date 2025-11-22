//package org.example.common.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.apache.rocketmq.spring.core.RocketMQTemplate;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.messaging.converter.MappingJackson2MessageConverter;

//@Configuration
//public class RocketMQConfig {
//    @Bean
//    public RocketMQTemplate rocketMQTemplate(RocketMQProducerFactory producerFactory) {
//        RocketMQTemplate template = new RocketMQTemplate(producerFactory);
//        // 配置 Jackson 支持 LocalDateTime
//        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
//        ObjectMapper objectMapper = new ObjectMapper()
//                .registerModule(new JavaTimeModule())
//                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        converter.setObjectMapper(objectMapper);
//        template.setMessageConverter(converter);
//        return template;
//    }
//}