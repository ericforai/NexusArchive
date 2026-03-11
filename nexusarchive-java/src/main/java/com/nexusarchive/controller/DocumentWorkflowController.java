// Input: Spring Web、Spring Security、Jakarta Validation、DocumentWorkflowService、Result
// Output: DocumentWorkflowController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.modules.document.api.dto.DocumentAssignmentDto;
import com.nexusarchive.modules.document.api.dto.DocumentAssignmentRequest;
import com.nexusarchive.modules.document.api.dto.DocumentLockDto;
import com.nexusarchive.modules.document.api.dto.DocumentLockRequest;
import com.nexusarchive.modules.document.api.dto.DocumentReminderDto;
import com.nexusarchive.modules.document.api.dto.DocumentReminderRequest;
import com.nexusarchive.modules.document.api.dto.DocumentSectionDto;
import com.nexusarchive.modules.document.api.dto.DocumentSectionUpdateRequest;
import com.nexusarchive.modules.document.api.dto.DocumentVersionCreateRequest;
import com.nexusarchive.modules.document.api.dto.DocumentVersionDto;
import com.nexusarchive.modules.document.app.DocumentWorkflowService;
import com.nexusarchive.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/documents/{projectId}")
@RequiredArgsConstructor
public class DocumentWorkflowController {

    private final DocumentWorkflowService documentWorkflowService;

    @GetMapping("/editor/sections/{id}")
    @PreAuthorize("hasAnyAuthority('archive:manage','archive:read','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<DocumentSectionDto> getSection(@PathVariable String projectId, @PathVariable("id") String sectionId) {
        return Result.success(documentWorkflowService.getSection(projectId, sectionId));
    }

    @PutMapping("/editor/sections/{id}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<DocumentSectionDto> updateSection(@PathVariable String projectId,
                                                    @PathVariable("id") String sectionId,
                                                    @Valid @RequestBody DocumentSectionUpdateRequest request,
                                                    HttpServletRequest httpServletRequest) {
        return Result.success(documentWorkflowService.upsertSection(projectId, sectionId, request, resolveUserId(httpServletRequest)));
    }

    @PostMapping("/editor/assignments")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<DocumentAssignmentDto> createAssignment(@PathVariable String projectId,
                                                          @Valid @RequestBody DocumentAssignmentRequest request,
                                                          HttpServletRequest httpServletRequest) {
        return Result.success(documentWorkflowService.createAssignment(projectId, request, resolveUserId(httpServletRequest)));
    }

    @PostMapping("/editor/locks")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<DocumentLockDto> createLock(@PathVariable String projectId,
                                              @Valid @RequestBody DocumentLockRequest request,
                                              HttpServletRequest httpServletRequest) {
        return Result.success(documentWorkflowService.createLock(
                projectId,
                request,
                resolveUserId(httpServletRequest),
                resolveUserName()
        ));
    }

    @PostMapping("/editor/reminders")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<DocumentReminderDto> createReminder(@PathVariable String projectId,
                                                      @Valid @RequestBody DocumentReminderRequest request,
                                                      HttpServletRequest httpServletRequest) {
        return Result.success(documentWorkflowService.createReminder(projectId, request, resolveUserId(httpServletRequest)));
    }

    @GetMapping("/versions")
    @PreAuthorize("hasAnyAuthority('archive:manage','archive:read','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<DocumentVersionDto>> listVersions(@PathVariable String projectId) {
        return Result.success(documentWorkflowService.listVersions(projectId));
    }

    @PostMapping("/versions")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<DocumentVersionDto> createVersion(@PathVariable String projectId,
                                                    @Valid @RequestBody DocumentVersionCreateRequest request,
                                                    HttpServletRequest httpServletRequest) {
        return Result.success(documentWorkflowService.createVersion(projectId, request, resolveUserId(httpServletRequest)));
    }

    @PostMapping("/versions/{versionId}/rollback")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<DocumentVersionDto> rollbackVersion(@PathVariable String projectId,
                                                      @PathVariable String versionId,
                                                      HttpServletRequest httpServletRequest) {
        return Result.success(documentWorkflowService.rollbackVersion(projectId, versionId, resolveUserId(httpServletRequest)));
    }

    private String resolveUserId(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr != null) {
            return userIdAttr.toString();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getId();
        }
        throw new BusinessException(ErrorCode.CANNOT_GET_CURRENT_USER);
    }

    private String resolveUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            if (details.getFullName() != null && !details.getFullName().isBlank()) {
                return details.getFullName();
            }
            return details.getUsername();
        }
        return "未知用户";
    }
}
