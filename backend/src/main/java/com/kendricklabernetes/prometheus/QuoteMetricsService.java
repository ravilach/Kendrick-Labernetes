package com.kendricklabernetes.prometheus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class QuoteMetricsService {
    private final Counter mongoCreate;
    private final Counter h2Create;
    private final Counter postgresCreate;
    private final Counter mongoRead;
    private final Counter h2Read;
    private final Counter postgresRead;
    private final Counter mongoDelete;
    private final Counter h2Delete;
    private final Counter postgresDelete;

    public QuoteMetricsService(MeterRegistry registry) {
    this.mongoCreate = Counter.builder("db_mongo_create_total")
        .description("Total number of create operations against MongoDB")
        .register(registry);
    this.h2Create = Counter.builder("db_h2_create_total")
        .description("Total number of create operations against H2")
        .register(registry);
    this.postgresCreate = Counter.builder("db_postgres_create_total")
        .description("Total number of create operations against Postgres")
        .register(registry);

    this.mongoRead = Counter.builder("db_mongo_read_total")
        .description("Total number of read operations against MongoDB")
        .register(registry);
    this.h2Read = Counter.builder("db_h2_read_total")
        .description("Total number of read operations against H2")
        .register(registry);
    this.postgresRead = Counter.builder("db_postgres_read_total")
        .description("Total number of read operations against Postgres")
        .register(registry);

    this.mongoDelete = Counter.builder("db_mongo_delete_total")
        .description("Total number of delete operations against MongoDB")
        .register(registry);
    this.h2Delete = Counter.builder("db_h2_delete_total")
        .description("Total number of delete operations against H2")
        .register(registry);
    this.postgresDelete = Counter.builder("db_postgres_delete_total")
        .description("Total number of delete operations against Postgres")
        .register(registry);
    }

    public void incrementMongoCreate() { mongoCreate.increment(); }
    public void incrementH2Create() { h2Create.increment(); }
    public void incrementPostgresCreate() { postgresCreate.increment(); }

    public void incrementMongoRead() { mongoRead.increment(); }
    public void incrementH2Read() { h2Read.increment(); }
    public void incrementPostgresRead() { postgresRead.increment(); }

    public void incrementMongoDelete() { mongoDelete.increment(); }
    public void incrementH2Delete() { h2Delete.increment(); }
    public void incrementPostgresDelete() { postgresDelete.increment(); }
}
