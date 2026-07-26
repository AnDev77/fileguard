package com.example.fileguard.extension;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_change_logs")
public class PolicyChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(nullable = false, length = 20)
    private String extension;

    private Long policyId;

    private Boolean beforeBlocked;

    private Boolean afterBlocked;

    @Column(length = 100)
    private String actor;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected PolicyChangeLog() {
    }

    private PolicyChangeLog(String action, String extension, Long policyId, Boolean beforeBlocked, Boolean afterBlocked, String reason) {
        this.action = action;
        this.extension = extension;
        this.policyId = policyId;
        this.beforeBlocked = beforeBlocked;
        this.afterBlocked = afterBlocked;
        this.actor = "system";
        this.reason = reason;
    }

    public static PolicyChangeLog added(ExtensionPolicy policy) {
        return new PolicyChangeLog("ADD", policy.getExtension(), policy.getId(), null, policy.isBlocked(), "Custom extension added");
    }

    public static PolicyChangeLog deleted(ExtensionPolicy policy) {
        return new PolicyChangeLog("DELETE", policy.getExtension(), policy.getId(), policy.isBlocked(), null, "Custom extension deleted");
    }

    public static PolicyChangeLog toggled(ExtensionPolicy policy, boolean beforeBlocked, boolean afterBlocked) {
        return new PolicyChangeLog("TOGGLE", policy.getExtension(), policy.getId(), beforeBlocked, afterBlocked, "Blocked flag changed");
    }

    public static PolicyChangeLog rejectedDelete(ExtensionPolicy policy) {
        return new PolicyChangeLog("REJECT_DELETE", policy.getExtension(), policy.getId(), policy.isBlocked(), policy.isBlocked(), "Fixed extension delete rejected");
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
