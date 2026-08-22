-- Backs the AI response cache in cash-management-service. One row per cache key
-- (a branchId for branch explanations, or the literal 'ADMIN_INSIGHTS' for the network
-- briefing). LAST_UPDATED is a plain timestamp column compared in Java — freshness is
-- decided by TTL arithmetic, never by asking the AI to reason about dates itself.
CREATE TABLE AI_INSIGHT_CACHE (
	CACHE_KEY     VARCHAR2(30) NOT NULL,
	PAYLOAD_JSON  CLOB NOT NULL,
	LAST_UPDATED  TIMESTAMP NOT NULL,
	CONSTRAINT PK_AI_INSIGHT_CACHE PRIMARY KEY (CACHE_KEY)
);
