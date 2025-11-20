package com.kendricklabernetes.repository.h2;

import org.springframework.data.jpa.repository.JpaRepository;
import com.kendricklabernetes.model.h2.QuoteH2;

public interface QuoteH2Repository extends JpaRepository<QuoteH2, Long> {
}
