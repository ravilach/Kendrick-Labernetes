package com.kendricklabernetes.controller;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final AtomicReference<String> requestedDbType = new AtomicReference<>(null);

    @PostMapping("/set-db-type")
    public ResponseEntity<?> setDbType(@RequestBody Map<String, String> body) {
        String dbType = body.get("dbType");
        if (dbType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "dbType is required"));
        }
        requestedDbType.set(dbType);
        String msg = String.format("Requested DB type set to '%s'. To apply globally, restart the application with DB_TYPE=%s and SPRING_PROFILES_ACTIVE=%s.", dbType, dbType, dbType);
        return ResponseEntity.ok(Map.of("message", msg));
    }

    @PostMapping("/test-sql-connection")
    public ResponseEntity<?> testSqlConnection(@RequestBody Map<String, String> body) {
        String url = body.get("connectionString");
        String user = body.getOrDefault("username", "");
        String pass = body.getOrDefault("password", "");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "connectionString is required"));
        }
        try (Connection conn = DriverManager.getConnection(url, user.isBlank() ? null : user, pass.isBlank() ? null : pass);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/exec-sql")
    public ResponseEntity<?> execSql(@RequestBody Map<String, String> body) {
        String url = body.get("connectionString");
        String user = body.getOrDefault("username", "");
        String pass = body.getOrDefault("password", "");
        String query = body.get("query");
        if (url == null || url.isBlank() || query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "connectionString and query are required"));
        }
        String qtrim = query.trim().toLowerCase(Locale.ROOT);
        if (!qtrim.startsWith("select")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only SELECT queries are allowed via the explorer for safety."));
        }
        try (Connection conn = DriverManager.getConnection(url, user.isBlank() ? null : user, pass.isBlank() ? null : pass);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            int limit = 100;
            int count = 0;
            while (rs.next() && count++ < limit) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(md.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            return ResponseEntity.ok(Map.of("rows", rows));
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test-mongo-connection")
    public ResponseEntity<?> testMongo(@RequestBody Map<String, String> body) {
        String uri = body.get("connectionString");
        if (uri == null || uri.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "connectionString is required"));
        }
        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase adminDb = client.getDatabase("admin");
            Document res = adminDb.runCommand(new Document("ping", 1));
            return ResponseEntity.ok(Map.of("ok", true, "result", res.toJson()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mongo-explore")
    public ResponseEntity<?> mongoExplore(@RequestBody Map<String, Object> body) {
        String uri = (String) body.get("connectionString");
        String dbName = (String) body.getOrDefault("database", "test");
        String collection = (String) body.get("collection");
        int limit = (int) (body.getOrDefault("limit", 25));
        if (uri == null || uri.isBlank() || collection == null || collection.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "connectionString and collection are required"));
        }
        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase db = client.getDatabase(dbName);
            FindIterable<Document> it = db.getCollection(collection).find().limit(limit);
            List<String> docs = new ArrayList<>();
            for (Document d : it) {
                docs.add(d.toJson());
            }
            return ResponseEntity.ok(Map.of("documents", docs));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

}
