package com.infy.tmwa.service;

import com.infy.tmwa.config.JwtService;
import com.infy.tmwa.entity.TokenBlocklist;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.repository.TokenBlocklistRepository;
import com.infy.tmwa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository           userRepository;
    private final TokenBlocklistRepository tokenBlocklistRepository;
    private final BCryptPasswordEncoder    passwordEncoder;
    private final JwtService               jwtService;

    // ── Profile ──────────────────────────────────────────────────────────

    public Map<String, Object> getProfile(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",          user.getId());
        map.put("fullName",    user.getFullName());
        map.put("email",       user.getEmail());
        map.put("bio",         user.getBio());
        map.put("avatarColor", user.getAvatarColor());
        map.put("theme",       user.getTheme());
        map.put("role",        user.getRole().name());
        return map;
    }

    public Map<String, Object> updateProfile(User user, Map<String, Object> body) {
        if (body.containsKey("fullName"))    user.setFullName((String) body.get("fullName"));
        if (body.containsKey("bio"))         user.setBio((String) body.get("bio"));
        if (body.containsKey("avatarColor")) user.setAvatarColor((String) body.get("avatarColor"));
        if (body.containsKey("email"))       user.setEmail((String) body.get("email"));
        userRepository.save(user);
        return getProfile(user);
    }

    // ── Password ─────────────────────────────────────────────────────────

    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ── Sessions ─────────────────────────────────────────────────────────

    public List<Map<String, Object>> getSessions(User user, String currentJti) {
        Map<String, Object> session = new HashMap<>();
        session.put("jti",     currentJti);
        session.put("current", true);
        session.put("device",  "Current session");
        session.put("loginAt", LocalDateTime.now().toString());
        return List.of(session);
    }

    public void revokeSession(User user, String jti) {
        if (!tokenBlocklistRepository.existsByJti(jti)) {
            tokenBlocklistRepository.save(
                    TokenBlocklist.builder()
                            .jti(jti)
                            .userId(user.getId())
                            .revokedAt(LocalDateTime.now())
                            .build()
            );
        }
    }

    public void revokeAllOtherSessions(User user, String currentJti) {
        tokenBlocklistRepository.revokeAllExcept(user.getId(), currentJti);
    }

    // ── Preferences ──────────────────────────────────────────────────────

    public void updatePreferences(User user, Map<String, Object> body) {
        if (body.containsKey("theme")) {
            user.setTheme((String) body.get("theme"));
        }
        if (body.containsKey("notifications")) {
            try {
                Object notif = body.get("notifications");
                if (notif instanceof Map<?, ?> notifMap) {
                    StringBuilder sb = new StringBuilder("{");
                    boolean first = true;
                    for (Map.Entry<?, ?> entry : notifMap.entrySet()) {
                        if (!first) sb.append(",");
                        sb.append("\"").append(entry.getKey()).append("\":")
                                .append(entry.getValue());
                        first = false;
                    }
                    sb.append("}");
                    user.setNotificationsJson(sb.toString());
                }
            } catch (Exception ignored) {}
        }
        userRepository.save(user);
    }

    // ── Delete ───────────────────────────────────────────────────────────

    public void deleteAccount(User user) {
        tokenBlocklistRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }
}