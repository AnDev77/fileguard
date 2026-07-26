package com.example.fileguard.extension;

import com.example.fileguard.common.error.BusinessException;
import com.example.fileguard.common.error.ErrorCode;
import com.example.fileguard.extension.dto.ExtensionPolicyResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExtensionPolicyService {

    private static final int CUSTOM_EXTENSION_LIMIT = 200;

    private final ExtensionPolicyRepository extensionPolicyRepository;
    private final PolicyChangeLogRepository policyChangeLogRepository;

    public ExtensionPolicyService(
            ExtensionPolicyRepository extensionPolicyRepository,
            PolicyChangeLogRepository policyChangeLogRepository
    ) {
        this.extensionPolicyRepository = extensionPolicyRepository;
        this.policyChangeLogRepository = policyChangeLogRepository;
    }

    @Transactional(readOnly = true)
    public List<ExtensionPolicyResponse> getPolicies() {
        return extensionPolicyRepository.findAllByOrderByFixedDescExtensionAsc()
                .stream()
                .map(ExtensionPolicyResponse::from)
                .toList();
    }

    @Transactional
    public ExtensionPolicyResponse addCustomExtension(String rawExtension) {
        String extension = ExtensionNormalizer.normalize(rawExtension);
        if (extensionPolicyRepository.existsByExtension(extension)) {
            throw new BusinessException(ErrorCode.EXTENSION_ALREADY_EXISTS);
        }
        if (extensionPolicyRepository.countByFixedFalse() >= CUSTOM_EXTENSION_LIMIT) {
            throw new BusinessException(ErrorCode.CUSTOM_EXTENSION_LIMIT_EXCEEDED);
        }

        ExtensionPolicy policy = extensionPolicyRepository.save(new ExtensionPolicy(extension, false, true));
        policyChangeLogRepository.save(PolicyChangeLog.added(policy));
        return ExtensionPolicyResponse.from(policy);
    }

    @Transactional
    public ExtensionPolicyResponse updateBlocked(String rawExtension, boolean blocked) {
        String extension = ExtensionNormalizer.normalize(rawExtension);
        ExtensionPolicy policy = extensionPolicyRepository.findByExtension(extension)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXTENSION_NOT_FOUND));

        boolean beforeBlocked = policy.isBlocked();
        policy.changeBlocked(blocked);
        policyChangeLogRepository.save(PolicyChangeLog.toggled(policy, beforeBlocked, blocked));
        return ExtensionPolicyResponse.from(policy);
    }

    @Transactional
    public void deleteCustomExtension(Long id) {
        ExtensionPolicy policy = extensionPolicyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXTENSION_NOT_FOUND));

        if (policy.isFixed()) {
            policyChangeLogRepository.save(PolicyChangeLog.rejectedDelete(policy));
            throw new BusinessException(ErrorCode.FIXED_EXTENSION_NOT_DELETABLE);
        }

        policyChangeLogRepository.save(PolicyChangeLog.deleted(policy));
        extensionPolicyRepository.delete(policy);
    }

    @Transactional(readOnly = true)
    public boolean isBlockedExtension(String extension) {
        if (extension == null) {
            return false;
        }

        return extensionPolicyRepository.findByExtension(extension)
                .map(ExtensionPolicy::isBlocked)
                .orElse(false);
    }
}
