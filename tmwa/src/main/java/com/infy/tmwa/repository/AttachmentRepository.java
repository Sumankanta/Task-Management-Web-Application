package com.infy.tmwa.repository;

import com.infy.tmwa.entity.Task;
import com.infy.tmwa.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    // ── List metadata only — fileData column excluded via DTO projection ──
    // Used by GET /api/tasks/{taskId}/attachments
    List<TaskAttachment> findByTask(Task task);

    // ── Count attachments per task — enforces max 5 files rule ──
    int countByTask(Task task);

    // ── Delete all attachments for a task (also handled by ON DELETE CASCADE) ──
    void deleteByTask(Task task);
}
