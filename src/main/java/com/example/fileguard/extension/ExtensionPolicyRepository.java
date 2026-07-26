package com.example.fileguard.extension;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExtensionPolicyRepository extends JpaRepository<ExtensionPolicy, Long> {

    Optional<ExtensionPolicy> findByExtension(String extension);

    boolean existsByExtension(String extension);

    long countByFixedFalse();

    List<ExtensionPolicy> findAllByOrderByFixedDescExtensionAsc();

    List<ExtensionPolicy> findByBlockedTrue();
}
