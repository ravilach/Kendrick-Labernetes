package com.kendricklabernetes.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quotes")
public class QuoteMongo {
    @Id
    private String id;
    private String quote;
    private String timestamp;
    private String ip;
    private int quoteNumber;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuote() { return quote; }
    public void setQuote(String q) { this.quote = q; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String t) { this.timestamp = t; }
    public String getIp() { return ip; }
    public void setIp(String i) { this.ip = i; }
    public int getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(int n) { this.quoteNumber = n; }
}
