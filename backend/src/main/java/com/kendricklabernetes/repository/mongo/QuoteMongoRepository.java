package com.kendricklabernetes.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.kendricklabernetes.model.mongo.QuoteMongo;

public interface QuoteMongoRepository extends MongoRepository<QuoteMongo, String> {
}
