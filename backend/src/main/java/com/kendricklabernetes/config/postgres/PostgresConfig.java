package com.kendricklabernetes.config.postgres;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kendricklabernetes.repository.h2.QuoteH2Repository;


@Configuration
@Profile("postgres")
@EnableJpaRepositories(basePackages = "com.kendricklabernetes.repository.h2")
public class PostgresConfig {
    private static final Logger log = LoggerFactory.getLogger(PostgresConfig.class);

    @Bean
    public CommandLineRunner postgresStartupLogger(QuoteH2Repository quoteH2Repository) {
        return args -> {
            log.info("Postgres profile active — initializing Postgres DB instrumentation");
            if (quoteH2Repository == null) {
                log.warn("Postgres JPA repository bean not available at startup");
                return;
            }
            try {
                long count = quoteH2Repository.count();
                log.info("Postgres quotes repository available — current row count: {}", count);
            } catch (Exception e) {
                log.warn("Unable to query Postgres repository at startup: {}", e.getMessage());
            }
        };
    }
}
