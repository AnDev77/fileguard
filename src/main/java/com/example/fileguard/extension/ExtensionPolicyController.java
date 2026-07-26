package com.example.fileguard.extension;

import com.example.fileguard.common.ApiResponse;
import com.example.fileguard.extension.dto.CreateCustomExtensionRequest;
import com.example.fileguard.extension.dto.ExtensionPolicyResponse;
import com.example.fileguard.extension.dto.UpdateBlockedRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/extensions")
public class ExtensionPolicyController {

    private final ExtensionPolicyService extensionPolicyService;

    public ExtensionPolicyController(ExtensionPolicyService extensionPolicyService) {
        this.extensionPolicyService = extensionPolicyService;
    }

    @GetMapping
    public ApiResponse<List<ExtensionPolicyResponse>> getPolicies() {
        return ApiResponse.ok(extensionPolicyService.getPolicies());
    }

    @PostMapping("/custom")
    public ApiResponse<ExtensionPolicyResponse> addCustomExtension(@Valid @RequestBody CreateCustomExtensionRequest request) {
        return ApiResponse.ok(extensionPolicyService.addCustomExtension(request.extension()), "커스텀 확장자를 추가했습니다.");
    }

    @PatchMapping("/{extension}/blocked")
    public ApiResponse<ExtensionPolicyResponse> updateBlocked(
            @PathVariable String extension,
            @Valid @RequestBody UpdateBlockedRequest request
    ) {
        return ApiResponse.ok(extensionPolicyService.updateBlocked(extension, request.blocked()), "차단 여부를 변경했습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCustomExtension(@PathVariable Long id) {
        extensionPolicyService.deleteCustomExtension(id);
        return ApiResponse.ok(null, "커스텀 확장자를 삭제했습니다.");
    }
}
