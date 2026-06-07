package com.portfolio.nexon.domain.apiclient.entity;

import com.portfolio.nexon.global.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.util.StringUtils;

@Entity
@Table(
	name = "api_clients",
	uniqueConstraints = @UniqueConstraint(name = "uk_api_clients_client_id", columnNames = "client_id")
)
public class ApiClient extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "client_id", nullable = false, length = 100)
	private String clientId;

	@Column(name = "api_key_hash", nullable = false, length = 128)
	private String apiKeyHash;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApiClientRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApiClientStatus status;

	protected ApiClient() {
	}

	public ApiClient(String clientId, String apiKeyHash, String name, ApiClientRole role) {
		validate(clientId, apiKeyHash, name, role);
		this.clientId = clientId;
		this.apiKeyHash = apiKeyHash;
		this.name = name;
		this.role = role;
		this.status = ApiClientStatus.ACTIVE;
	}

	public void changeStatus(ApiClientStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("API client status must not be null");
		}

		this.status = status;
	}

	public void rotateApiKeyHash(String apiKeyHash) {
		if (!StringUtils.hasText(apiKeyHash)) {
			throw new IllegalArgumentException("API key hash must not be empty");
		}

		this.apiKeyHash = apiKeyHash;
	}

	public boolean isActive() {
		return status == ApiClientStatus.ACTIVE;
	}

	public Long getId() {
		return id;
	}

	public String getClientId() {
		return clientId;
	}

	public String getApiKeyHash() {
		return apiKeyHash;
	}

	public String getName() {
		return name;
	}

	public ApiClientRole getRole() {
		return role;
	}

	public ApiClientStatus getStatus() {
		return status;
	}

	private void validate(String clientId, String apiKeyHash, String name, ApiClientRole role) {
		if (!StringUtils.hasText(clientId)) {
			throw new IllegalArgumentException("API client id must not be empty");
		}
		if (!StringUtils.hasText(apiKeyHash)) {
			throw new IllegalArgumentException("API key hash must not be empty");
		}
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("API client name must not be empty");
		}
		if (role == null) {
			throw new IllegalArgumentException("API client role must not be null");
		}
	}
}
