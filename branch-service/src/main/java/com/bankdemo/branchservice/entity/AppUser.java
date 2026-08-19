package com.bankdemo.branchservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "APP_USER")
public class AppUser {
	@Id
	@Column(name = "USER_ID", length = 36)
	private String userId;
	@Column(name = "USERNAME", nullable = false, unique = true, length = 50)
	private String username;
	@Column(name = "PASSWORD_HASH", nullable = false, length = 255)
	private String passwordHash;
	@Column(name = "CREATED_AT")
	private LocalDateTime createdAt;

	public String getUserId() { return userId; }
	public void setUserId(String userId) { this.userId = userId; }
	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public String getPasswordHash() { return passwordHash; }
	public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
