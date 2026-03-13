package com.infy.tmwa.service;

import com.infy.tmwa.dto.AttachmentListDTO;
import com.infy.tmwa.entity.Task;
import com.infy.tmwa.entity.TaskAttachment;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.entity.UserRole;
import com.infy.tmwa.repository.AttachmentRepository;
import com.infy.tmwa.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository       taskRepository;

    // ── Allowed MIME types per SRS ──
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "application/zip"
    );

    private static final long MAX_FILE_SIZE  = 5L * 1024 * 1024; // 5 MB
    private static final int  MAX_FILES_TASK = 5;

    // ── Upload ──
    public AttachmentListDTO upload(Long taskId, MultipartFile file, User uploader)
            throws IOException {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        // Server-side file size check (client also validates)
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File exceeds 5 MB limit");
        }

        // MIME type check
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_TYPES.contains(mimeType)) {
            throw new RuntimeException("File type not allowed: " + mimeType);
        }

        // Max 5 files per task check
        int existingCount = attachmentRepository.countByTask(task);
        if (existingCount >= MAX_FILES_TASK) {
            throw new RuntimeException("Maximum 5 files reached for this task");
        }

        // Store BLOB in DB
        TaskAttachment attachment = TaskAttachment.builder()
                .task(task)
                .uploader(uploader)
                .originalName(file.getOriginalFilename())
                .mimeType(mimeType)
                .fileSizeBytes(file.getSize())
                .fileData(file.getBytes()) // ← actual bytes stored as LONGBLOB
                .build();

        TaskAttachment saved = attachmentRepository.save(attachment);
        log.info("Attachment '{}' uploaded for task {} by user {}",
                saved.getOriginalName(), taskId, uploader.getEmail());

        return toListDTO(saved);
    }

    // ── List metadata only — NEVER return fileData in list ──
    public List<AttachmentListDTO> listAttachments(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        return attachmentRepository.findByTask(task)
                .stream()
                .map(this::toListDTO)
                .collect(Collectors.toList());
    }

    // ── Download — loads one row including BLOB ──
    public TaskAttachment download(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found: " + attachmentId));
    }

    // ── Delete — Admin/Manager/uploader only ──
    public void delete(Long attachmentId, User user) {
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found: " + attachmentId));

        boolean isOwner   = attachment.getUploader().getId().equals(user.getId());
        boolean isPrivileged = user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.MANAGER;

        if (!isOwner && !isPrivileged) {
            throw new RuntimeException("Access denied: cannot delete this attachment");
        }

        attachmentRepository.delete(attachment);
        log.info("Attachment {} deleted by user {}", attachmentId, user.getEmail());
    }

    // ── Map entity → DTO (no fileData) ──
    private AttachmentListDTO toListDTO(TaskAttachment a) {
        return AttachmentListDTO.builder()
                .id(a.getId())
                .taskId(a.getTask().getId())
                .uploaderId(a.getUploader().getId())
                .uploaderName(a.getUploader().getFullName())
                .originalName(a.getOriginalName())
                .mimeType(a.getMimeType())
                .fileSizeBytes(a.getFileSizeBytes())
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}