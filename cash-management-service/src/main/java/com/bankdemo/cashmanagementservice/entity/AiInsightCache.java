package com.bankdemo.cashmanagementservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "AI_INSIGHT_CACHE")
public class AiInsightCache {
	@Id
	@Column(name = "CACHE_KEY", length = 30)
	private String cacheKey;
	@Lob
	@Column(name = "PAYLOAD_JSON", nullable = false)
	private String payloadJson;
	@Column(name = "LAST_UPDATED", nullable = false)
	private LocalDateTime lastUpdated;

	public String getCacheKey() { return cacheKey; }
	public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }
	public String getPayloadJson() { return payloadJson; }
	public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
	public LocalDateTime getLastUpdated() { return lastUpdated; }
	public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
