package com.portfolio.nexon.domain.apiclient.repository;

import com.portfolio.nexon.domain.apiclient.entity.ApiClient;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiClientRepository extends JpaRepository<ApiClient, Long> {

	Optional<ApiClient> findByClientId(String clientId);

	boolean existsByClientId(String clientId);

	boolean existsByName(String name);
}
