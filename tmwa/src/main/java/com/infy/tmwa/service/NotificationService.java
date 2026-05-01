package com.infy.tmwa.service;

import com.infy.tmwa.entity.Notification;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ── Create ────────────────────────────────────────────────────
    public void create(User recipient, String title, String body, String type) {
        if (recipient == null) return;
        Notification n = Notification.builder()
                .user(recipient)
                .title(title)
                .body(body)
                .type(type)
                .build();
        notificationRepository.save(n);
        log.info("Notification created for {}: [{}] {}", recipient.getEmail(), type, title);
    }

    // ── GET all for user ──────────────────────────────────────────
    public List<Notification> getForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // ── Unread count ──────────────────────────────────────────────
    public int unreadCount(User user) {
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    // ── Mark single as read ───────────────────────────────────────
    @Transactional
    public void markRead(Long id, User user) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUser().getId().equals(user.getId())) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    // ── Mark ALL as read ──────────────────────────────────────────
    @Transactional
    public void markAllRead(User user) {
        notificationRepository.markAllReadByUser(user);
    }

    // ── Delete single ─────────────────────────────────────────────
    @Transactional
    public void delete(Long id, User user) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUser().getId().equals(user.getId())) {
                notificationRepository.delete(n);
            }
        });
    }

    // ── Clear ALL for user ────────────────────────────────────────
    @Transactional
    public void clearAll(User user) {
        notificationRepository.deleteByUser(user);
    }
}

