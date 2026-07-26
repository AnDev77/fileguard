package com.example.fileguard.extension;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyChangeLogRepository extends JpaRepository<PolicyChangeLog, Long> {
}
