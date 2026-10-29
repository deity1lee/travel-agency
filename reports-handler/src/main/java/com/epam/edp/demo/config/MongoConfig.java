package com.epam.edp.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * MongoDB wiring for the tours and reviews stores.
 *
 * <p>The {@link MongoDatabaseFactory} (and its underlying {@code MongoClient}) is provided by Spring
 * Boot autoconfiguration from {@code spring.data.mongodb.*}; defining a factory bean here would make
 * {@code MongoAutoConfiguration} back off entirely, taking the {@code MongoClient} bean with it.
 *
 * <p>The {@link MongoTransactionManager} enables the multi-document transactions used when a
 * review write and its tour rating-aggregate update must succeed or fail together
 * ({@code ReviewRepositoryImpl}). Multi-document transactions require the MongoDB deployment to be
 * a replica set (or sharded cluster); a standalone {@code mongod} will reject them at runtime.
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }

    @Bean
    public TransactionTemplate mongoTransactionTemplate(MongoTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
