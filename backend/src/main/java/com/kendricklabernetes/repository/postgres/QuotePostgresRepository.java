package com.kendricklabernetes.repository.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import com.kendricklabernetes.model.postgres.QuotePostgres;

public interface QuotePostgresRepository extends JpaRepository<QuotePostgres, Long> {
}
