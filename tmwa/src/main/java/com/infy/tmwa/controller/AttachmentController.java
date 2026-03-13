package com.infy.tmwa.controller;

import com.infy.tmwa.dto.AttachmentListDTO;
import com.infy.tmwa.entity.TaskAttachment;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class AttachmentController {

    private final AttachmentService attachmentService;

    // ── POST /api/tasks/{taskId}/attachments ──
    // Accepts multipart/form-data with a single "file" field
    // Admin/Manager/Member only (Viewer blocked in frontend + @PreAuthorize later)
    @PostMapping("/api/tasks/{taskId}/attachments")
    public ResponseEntity<AttachmentListDTO> upload(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {

        log.info("Upload request: task={}, file='{}', size={}, user={}",
                taskId, file.getOriginalFilename(), file.getSize(), user.getEmail());

        AttachmentListDTO dto = attachmentService.upload(taskId, file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // ── GET /api/tasks/{taskId}/attachments ──
    // Returns metadata list only — fileData NEVER included
    @GetMapping("/api/tasks/{taskId}/attachments")
    public ResponseEntity<List<AttachmentListDTO>> list(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {

        log.info("List attachments: task={}, user={}", taskId, user.getEmail());
        return ResponseEntity.ok(attachmentService.listAttachments(taskId));
    }

    // ── GET /api/attachments/{attachmentId}/download ──
    // Fetches one row including BLOB, returns file bytes with correct headers
    @GetMapping("/api/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal User user) {

        log.info("Download request: attachment={}, user={}", attachmentId, user.getEmail());

        TaskAttachment attachment = attachmentService.download(attachmentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getOriginalName() + "\"")
                .body(attachment.getFileData());
    }

    // ── DELETE /api/attachments/{attachmentId} ──
    // Admin/Manager/uploader only — enforced in service
    @DeleteMapping("/api/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal User user) {

        log.info("Delete attachment: id={}, user={}", attachmentId, user.getEmail());
        attachmentService.delete(attachmentId, user);
        return ResponseEntity.noContent().build();
    }
}
