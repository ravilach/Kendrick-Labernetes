// Main Spring Boot application class for the Kendrick Labernetes application
package com.kendricklabernetes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.context.annotation.Import;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

class KendrickLabernetesConfigSelector implements ImportSelector, EnvironmentAware {
    private Environment environment;
    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }
    public @NonNull String[] selectImports(@NonNull AnnotationMetadata importingClassMetadata) {
        String dbType = environment != null ? environment.getProperty("DB_TYPE", "h2") : "h2";
        if ("mongo".equalsIgnoreCase(dbType)) {
            return new String[]{"com.kendricklabernetes.config.mongo.MongoConfig"};
        } else if ("postgres".equalsIgnoreCase(dbType)) {
            return new String[]{"com.kendricklabernetes.config.postgres.PostgresConfig"};
        } else {
            return new String[]{"com.kendricklabernetes.config.h2.H2Config"};
        }
    }
}
/**
 * Main Spring Boot application class for Kendrick Labernetes.
 *
 * This class uses an ImportSelector to dynamically import the configuration
 * class for the chosen persistence implementation based on the `DB_TYPE`
 * environment/property (supported values: `h2`, `mongo`, `postgres`).
 */
@SpringBootApplication
@Import(KendrickLabernetesConfigSelector.class)
public class KendrickLabernetesApplication {
    /**
     * Application entry point.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(KendrickLabernetesApplication.class, args);
    }
}
