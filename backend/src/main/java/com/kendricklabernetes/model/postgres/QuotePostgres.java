package com.kendricklabernetes.model.postgres;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class QuotePostgres {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String quote;
    private String timestamp;
    private String ip;
    private int quoteNumber;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuote() { return quote; }
    public void setQuote(String q) { this.quote = q; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String t) { this.timestamp = t; }
    public String getIp() { return ip; }
    public void setIp(String i) { this.ip = i; }
    public int getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(int n) { this.quoteNumber = n; }
}
