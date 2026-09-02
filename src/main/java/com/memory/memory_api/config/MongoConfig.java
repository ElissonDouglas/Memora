package com.memory.memory_api.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoConfig {

    private static final String DATABASE_NAME = "memory_api_db";
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";

    @Bean
    public SimpleMongoClientDatabaseFactory mongoDbFactory() {
        MongoClient mongoClient = MongoClients.create(CONNECTION_STRING);
        return new SimpleMongoClientDatabaseFactory(mongoClient, DATABASE_NAME);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoDbFactory());
    }
}