package com.kendricklabernetes.config.mongo;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.kendricklabernetes.repository.mongo")
public class MongoConfig {
    // Mongo-specific beans/configuration
}
