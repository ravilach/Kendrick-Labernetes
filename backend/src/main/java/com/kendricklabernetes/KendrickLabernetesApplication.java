// Main Spring Boot application class for Kendrick Labernetes
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
        String remoteDb = environment != null ? environment.getProperty("REMOTE_DB", "false") : "false";
        if ("true".equalsIgnoreCase(remoteDb)) {
            return new String[]{"com.kendricklabernetes.config.mongo.MongoConfig"};
        } else {
            return new String[]{"com.kendricklabernetes.config.h2.H2Config"};
        }
    }
}
/**
 * Main Spring Boot application class for Kendrick-Labernetes.
 * Dynamically imports MongoDB or H2 config based on REMOTE_DB flag.
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
