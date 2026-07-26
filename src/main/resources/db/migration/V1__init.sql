CREATE TABLE extension_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    extension VARCHAR(20) NOT NULL,
    fixed BOOLEAN NOT NULL,
    blocked BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_extension_policies_extension UNIQUE (extension),
    INDEX idx_extension_policies_blocked (blocked),
    INDEX idx_extension_policies_fixed (fixed)
);

CREATE TABLE policy_change_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(30) NOT NULL,
    extension VARCHAR(20) NOT NULL,
    policy_id BIGINT NULL,
    before_blocked BOOLEAN NULL,
    after_blocked BOOLEAN NULL,
    actor VARCHAR(100) NULL,
    reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_policy_change_logs_extension (extension),
    INDEX idx_policy_change_logs_created_at (created_at),
    CONSTRAINT fk_policy_change_logs_policy
        FOREIGN KEY (policy_id) REFERENCES extension_policies(id)
        ON DELETE SET NULL
);

CREATE TABLE uploaded_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    extension VARCHAR(20) NULL,
    content_type VARCHAR(100) NULL,
    size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reject_reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_uploaded_files_extension (extension),
    INDEX idx_uploaded_files_status (status),
    INDEX idx_uploaded_files_created_at (created_at)
);

INSERT INTO extension_policies (extension, fixed, blocked, created_at, updated_at)
VALUES
    ('bat', TRUE, FALSE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('cmd', TRUE, FALSE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('com', TRUE, FALSE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('cpl', TRUE, FALSE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('exe', TRUE, FALSE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('scr', TRUE, FALSE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('js', TRUE, FALSE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
