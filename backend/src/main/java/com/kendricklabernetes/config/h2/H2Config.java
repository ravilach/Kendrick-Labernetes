package com.kendricklabernetes.config.h2;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kendricklabernetes.repository.h2.QuoteH2Repository;


@Configuration
@Profile("h2")
@EnableJpaRepositories(basePackages = "com.kendricklabernetes.repository.h2")
public class H2Config {
    private static final Logger log = LoggerFactory.getLogger(H2Config.class);

    @Bean
    public CommandLineRunner h2StartupLogger(QuoteH2Repository quoteH2Repository) {
        return args -> {
            log.info("H2 profile active — initializing H2 DB instrumentation");
            if (quoteH2Repository == null) {
                log.warn("H2 JPA repository bean not available at startup");
                return;
            }
            try {
                long count = quoteH2Repository.count();
                log.info("H2 quotes repository available — current row count: {}", count);
            } catch (Exception e) {
                log.warn("Unable to query H2 repository at startup: {}", e.getMessage());
            }
        };
    }
}
