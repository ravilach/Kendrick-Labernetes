package com.kendricklabernetes.config.h2;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.kendricklabernetes.repository.h2")
public class H2Config {
    // H2-specific beans/configuration
}
