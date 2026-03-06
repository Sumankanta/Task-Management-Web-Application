package com.infy.tmwa.service;

import com.infy.tmwa.entity.Task;
import com.infy.tmwa.entity.TaskComment;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.repository.CommentRepository;
import com.infy.tmwa.repository.TaskRepository;
import com.infy.tmwa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository    taskRepository;
    private final UserRepository    userRepository;
    private final ActivityService   activityService;   // ← injected

    public List<TaskComment> getComments(Long taskId) {
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public TaskComment addComment(Long taskId, String body, String email) {

        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TaskComment comment = TaskComment.builder()
                .task(task)
                .author(user)
                .body(body)
                .build();

        TaskComment saved = commentRepository.save(comment);

        // ── F-EXT-05: Log COMMENT_ADDED ──
        activityService.logActivity(
                user,
                taskId,
                "COMMENT_ADDED",
                user.getFullName() + " commented on \"" + task.getTitle() + "\""
        );

        return saved;
    }

    public void deleteComment(Long commentId, String email) {

        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthor().getEmail().equals(email)) {
            throw new AccessDeniedException("You cannot delete this comment");
        }

        commentRepository.delete(comment);
        // Note: no activity log for comment deletion — not in F-EXT-05 spec
    }
}