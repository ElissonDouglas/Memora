package com.memory.memora_api.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoConfig {

    private static final String DATABASE_NAME = "memora_api_db";

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017}")
    private String connectionUri;

    @Bean
    public SimpleMongoClientDatabaseFactory mongoDbFactory() {
        MongoClient mongoClient = MongoClients.create(connectionUri);
        return new SimpleMongoClientDatabaseFactory(mongoClient, DATABASE_NAME);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoDbFactory());
    }
}