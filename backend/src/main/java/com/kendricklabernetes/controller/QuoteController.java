// REST API controller for quote operations and node info
package com.kendricklabernetes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.kendricklabernetes.model.mongo.QuoteMongo;
import com.kendricklabernetes.model.h2.QuoteH2;
import com.kendricklabernetes.repository.mongo.QuoteMongoRepository;
import com.kendricklabernetes.repository.h2.QuoteH2Repository;
import org.springframework.dao.DataAccessResourceFailureException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API controller for quote operations and node/application info endpoints.
 * Handles both MongoDB and H2 persistence based on REMOTE_DB flag.
 */
@RestController
@RequestMapping("/api")
public class QuoteController {
    private static final Logger logger = LoggerFactory.getLogger(QuoteController.class);

    @Autowired
    private com.kendricklabernetes.prometheus.QuoteMetricsService quoteMetricsService;
    @Autowired
    private org.springframework.core.env.Environment env;
    @Autowired
    private org.springframework.context.ApplicationContext ctx;

    @PostMapping("/quotes")
    public ResponseEntity<?> addQuote(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        logger.info("addQuote called with payload: {}", payload);
        boolean useMongo = Boolean.parseBoolean(env.getProperty("REMOTE_DB", "false"));
        if (useMongo) {
            QuoteMongoRepository repo = null;
            try {
                repo = ctx.getBean(QuoteMongoRepository.class);
            } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException ex) {
                logger.info("MongoDB repository unavailable in addQuote");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorResponse("MongoDB connection unavailable at configured URL."));
            }
            try {
                String quoteText = payload.get("quote");
                QuoteMongo quote = new QuoteMongo();
                quote.setQuote(quoteText);
                quote.setTimestamp(Instant.now().toString());
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                quote.setIp(ip);
                quote.setQuoteNumber(getNextQuoteNumber());
                try {
                    logger.info("Attempting to save new quote to MongoDB: {}", quote.getQuote());
                    QuoteMongo saved = repo.save(quote);
                    quoteMetricsService.incrementMongoCreate();
                    logger.info("Saved quote to MongoDB with id: {}", saved.getId());
                    return ResponseEntity.ok(saved);
                } catch (Exception e) {
                    logger.error("Failed to save quote to MongoDB: {}", e.getMessage(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse("Failed to save quote: " + e.getMessage()));
                }
            } catch (Exception e) {
                logger.error("Exception in addQuote (Mongo): {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Failed to save quote: " + e.getMessage()));
            }
        } else {
            QuoteH2Repository repo = null;
            try {
                repo = ctx.getBean(QuoteH2Repository.class);
            } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException ex) {
                logger.info("H2/JPA repository unavailable in addQuote");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorResponse("H2/JPA repository unavailable."));
            }
            try {
                String quoteText = payload.get("quote");
                QuoteH2 quote = new QuoteH2();
                quote.setQuote(quoteText);
                quote.setTimestamp(Instant.now().toString());
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                quote.setIp(ip);
                quote.setQuoteNumber(getNextQuoteNumber());
                logger.info("Attempting to save new quote to H2: {}", quote.getQuote());
                QuoteH2 saved = repo.save(quote);
                quoteMetricsService.incrementH2Create();
                logger.info("Saved quote to H2 with id: {}", saved.getId());
                return ResponseEntity.ok(saved);
            } catch (Exception e) {
                logger.error("Exception in addQuote (H2): {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Failed to save quote: " + e.getMessage()));
            }
        }
    }

    @GetMapping("/quotes/latest")
    public ResponseEntity<?> getLatestQuote() {
        logger.info("getLatestQuote called");
        boolean useMongo = Boolean.parseBoolean(env.getProperty("REMOTE_DB", "false"));
        if (useMongo) {
            QuoteMongoRepository repo = null;
            try {
                repo = ctx.getBean(QuoteMongoRepository.class);
            } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException ex) {
                logger.info("MongoDB repository unavailable in getLatestQuote");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorResponse("MongoDB connection unavailable at configured URL."));
            }
            try {
                long count = repo.count();
                logger.info("MongoDB quote count: {}", count);
                if (count == 0) {
                    logger.info("No quotes found in MongoDB");
                    return ResponseEntity.ok().body(null);
                }
                QuoteMongo latest = null;
                try {
                    logger.info("Fetching all quotes from MongoDB to compute latest");
                    latest = repo.findAll()
                        .stream()
                        .max((a, b) -> Integer.compare(a.getQuoteNumber(), b.getQuoteNumber()))
                        .orElse(null);
                    quoteMetricsService.incrementMongoRead();
                    logger.info("Fetched latest quote from MongoDB: {}", latest);
                } catch (Exception e) {
                    logger.error("Failed to fetch latest quote from MongoDB: {}", e.getMessage(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse("Failed to fetch latest quote: " + e.getMessage()));
                }
                return ResponseEntity.ok(latest);
            } catch (DataAccessResourceFailureException ex) {
                logger.error("MongoDB connection unavailable: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorResponse("MongoDB connection unavailable at configured URL."));
            } catch (Exception ex) {
                logger.error("Unexpected error during MongoDB read: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Unexpected error: " + ex.getMessage()));
            }
        } else {
            QuoteH2Repository repo = null;
            try {
                repo = ctx.getBean(QuoteH2Repository.class);
            } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException ex) {
                logger.info("H2/JPA repository unavailable in getLatestQuote");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorResponse("H2/JPA repository unavailable."));
            }
            try {
                long count = repo.count();
                logger.info("H2 quote count: {}", count);
                if (count == 0) {
                    logger.info("No quotes found in H2");
                    return ResponseEntity.ok().body(null);
                }
                logger.info("Fetching all quotes from H2 to compute latest");
                QuoteH2 latest = repo.findAll()
                    .stream()
                    .max((a, b) -> Integer.compare(a.getQuoteNumber(), b.getQuoteNumber()))
                    .orElse(null);
                quoteMetricsService.incrementH2Read();
                logger.info("Fetched latest quote from H2: {}", latest);
                return ResponseEntity.ok(latest);
            } catch (Exception e) {
                logger.error("Exception in getLatestQuote (H2): {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Failed to fetch latest quote: " + e.getMessage()));
            }
        }
    }

    @GetMapping("/quotes")
    public ResponseEntity<?> getAllQuotes() {
        logger.info("getAllQuotes called");
        boolean useMongo = Boolean.parseBoolean(env.getProperty("REMOTE_DB", "false"));
        if (useMongo) {
            QuoteMongoRepository repo = getMongoRepo();
            if (repo == null) {
                logger.info("MongoDB repository unavailable in getAllQuotes");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorResponse("MongoDB connection unavailable at configured URL."));
            }
            try {
                logger.info("Fetching all quotes from MongoDB");
                var all = repo.findAll();
                quoteMetricsService.incrementMongoRead();
                logger.info("Fetched {} quotes from MongoDB", all.size());
                return ResponseEntity.ok(all);
            } catch (Exception e) {
                logger.error("Exception in getAllQuotes (Mongo): {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Failed to fetch quotes: " + e.getMessage()));
            }
        } else {
            QuoteH2Repository repo = getH2Repo();
            if (repo == null) {
                logger.info("H2/JPA repository unavailable in getAllQuotes");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorResponse("H2/JPA repository unavailable."));
            }
            try {
                logger.info("Fetching all quotes from H2");
                var all = repo.findAll();
                quoteMetricsService.incrementH2Read();
                logger.info("Fetched {} quotes from H2", all.size());
                return ResponseEntity.ok(all);
            } catch (Exception e) {
                logger.error("Exception in getAllQuotes (H2): {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Failed to fetch quotes: " + e.getMessage()));
            }
        }
    }

    @DeleteMapping("/quotes/{id}")
    public ResponseEntity<?> deleteQuote(@PathVariable("id") String id) {
        logger.info("deleteQuote called with id: {}", id);
        boolean useMongo = Boolean.parseBoolean(env.getProperty("REMOTE_DB", "false"));
        try {
            if (useMongo) {
                QuoteMongoRepository repo = getMongoRepo();
                if (repo == null) {
                    logger.info("MongoDB repository unavailable in deleteQuote");
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(errorResponse("MongoDB connection unavailable at configured URL."));
                }
                logger.info("Deleting quote id {} from MongoDB", id);
                repo.deleteById(id);
                quoteMetricsService.incrementMongoDelete();
                logger.info("Deleted quote from MongoDB with id: {}", id);
                return ResponseEntity.ok().body("Deleted");
            } else {
                QuoteH2Repository repo = getH2Repo();
                if (repo == null) {
                    logger.info("H2/JPA repository unavailable in deleteQuote");
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(errorResponse("H2/JPA repository unavailable."));
                }
                logger.info("Deleting quote id {} from H2", id);
                repo.deleteById(Long.parseLong(id));
                quoteMetricsService.incrementH2Delete();
                logger.info("Deleted quote from H2 with id: {}", id);
                return ResponseEntity.ok().body("Deleted");
            }
        } catch (Exception e) {
            logger.error("Exception in deleteQuote: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("Failed to delete quote: " + e.getMessage()));
        }
    }

    @GetMapping("/nodeinfo")
    public Map<String, Object> getNodeInfo() {
        logger.info("getNodeInfo called");
        Map<String, Object> info = new HashMap<>();
        info.put("hostname", getHostName());
        info.put("app", "Kendrick-Labernetes");
        info.put("os.name", System.getProperty("os.name"));
        info.put("os.version", System.getProperty("os.version"));
        info.put("os.arch", System.getProperty("os.arch"));
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        info.put("maxMemoryMB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        info.put("totalMemoryMB", Runtime.getRuntime().totalMemory() / (1024 * 1024));
        info.put("freeMemoryMB", Runtime.getRuntime().freeMemory() / (1024 * 1024));
        info.put("timestamp", Instant.now().toString());
        return info;
    }

    @GetMapping("/dbstatus")
    public Map<String, String> getDbStatus() {
        logger.info("getDbStatus called");
        boolean useMongo = Boolean.parseBoolean(env.getProperty("REMOTE_DB", "false"));
        Map<String, String> status = new HashMap<>();
        if (useMongo) {
            status.put("type", "MongoDB");
            status.put("connected", getMongoRepo() != null ? "true" : "false");
            status.put("message", getMongoRepo() != null ? "Connected to MongoDB" : "MongoDB repository unavailable");
        } else {
            status.put("type", "H2");
            status.put("connected", getH2Repo() != null ? "true" : "false");
            status.put("message", getH2Repo() != null ? "Connected to H2" : "H2 repository unavailable");
        }
        return status;
    }

    private Map<String, String> errorResponse(String msg) {
        logger.info("errorResponse called with msg: {}", msg);
        Map<String, String> err = new HashMap<>();
        err.put("error", msg);
        return err;
    }

    private String getHostName() {
        logger.info("getHostName called");
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            logger.error("Exception in getHostName: {}", e.getMessage(), e);
            return "unknown";
        }
    }

    private int getNextQuoteNumber() {
        logger.info("getNextQuoteNumber called");
        boolean useMongo = Boolean.parseBoolean(env.getProperty("REMOTE_DB", "false"));
        if (useMongo && getMongoRepo() != null) {
            return (int) (getMongoRepo().count() + 1);
        } else if (getH2Repo() != null) {
            return (int) (getH2Repo().count() + 1);
        } else {
            return 1;
        }
    }

    private QuoteMongoRepository getMongoRepo() {
        try {
            return ctx.getBean(QuoteMongoRepository.class);
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException ex) {
            return null;
        }
    }

    private QuoteH2Repository getH2Repo() {
        try {
            return ctx.getBean(QuoteH2Repository.class);
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException ex) {
            return null;
        }
    }
}
