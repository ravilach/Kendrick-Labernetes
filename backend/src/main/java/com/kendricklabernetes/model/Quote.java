package com.kendricklabernetes.model;

public class Quote {
    private String quote;
    private String timestamp;
    private String ip;
    private int quoteNumber;

    public String getQuote() { return quote; }
    public void setQuote(String q) { this.quote = q; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String t) { this.timestamp = t; }
    public String getIp() { return ip; }
    public void setIp(String i) { this.ip = i; }
    public int getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(int n) { this.quoteNumber = n; }
}
