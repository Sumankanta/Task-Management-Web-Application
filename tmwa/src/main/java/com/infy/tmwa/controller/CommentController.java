package com.infy.tmwa.controller;

import com.infy.tmwa.entity.TaskComment;
import com.infy.tmwa.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/tasks/{taskId}/comments")
    public List<TaskComment> getComments(@PathVariable Long taskId) {
        return commentService.getComments(taskId);
    }

    @PostMapping("/tasks/{taskId}/comments")
    public ResponseEntity<TaskComment> addComment(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        TaskComment saved = commentService.addComment(
                taskId,
                request.get("body"),
                authentication.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            Authentication authentication) {

        commentService.deleteComment(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}