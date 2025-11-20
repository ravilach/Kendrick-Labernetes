package com.kendricklabernetes.config.mongo;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kendricklabernetes.repository.mongo.QuoteMongoRepository;
import org.springframework.core.env.Environment;

@Configuration
@Profile("mongo")
@EnableMongoRepositories(basePackages = "com.kendricklabernetes.repository.mongo")
public class MongoConfig {
    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Bean
    public CommandLineRunner mongoStartupLogger(QuoteMongoRepository quoteMongoRepository, Environment env) {
        return args -> {
            log.info("mongo profile active — initializing MongoDB instrumentation");
            String uri = env.getProperty("spring.data.mongodb.uri", env.getProperty("MONGODB_URI", "(not set)"));
            log.info("MongoDB URI (from config): {}", uri);
            if (quoteMongoRepository == null) {
                log.warn("Mongo repository bean not available at startup");
                return;
            }
            try {
                long count = quoteMongoRepository.count();
                log.info("MongoDB quotes repository available — current document count: {}", count);
            } catch (Exception e) {
                log.warn("Unable to query Mongo repository at startup: {}", e.getMessage());
            }
        };
    }
}
