package com.infy.tmwa.controller;

import com.infy.tmwa.dto.TeamDTO;
import com.infy.tmwa.entity.Team;
import com.infy.tmwa.entity.TeamMember;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.repository.TaskRepository;
import com.infy.tmwa.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class TeamController {

    private final TeamService    teamService;
    private final TaskRepository taskRepository;

    // ── POST /api/teams ──
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTeam(
            @RequestBody TeamDTO dto,
            @AuthenticationPrincipal User user) {
        log.info("Create team: name='{}', user={}", dto.getName(), user.getEmail());
        Team team = teamService.createTeam(dto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(team));
    }

    // ── GET /api/teams ──
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getTeams(
            @AuthenticationPrincipal User user) {
        log.info("Get teams for user={}, role={}", user.getEmail(), user.getRole());
        List<Map<String, Object>> result = teamService.getTeams(user)
                .stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ── GET /api/teams/{id} ──
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toMap(teamService.getTeamById(id)));
    }

    // ── POST /api/teams/{id}/members ──
    @PostMapping("/{id}/members")
    public ResponseEntity<TeamMember> addMember(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal User user) {
        Long userId = body.get("userId");
        log.info("Add member: team={}, userId={}, by={}", id, userId, user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.addMember(id, userId, user));
    }

    // ── DELETE /api/teams/{id}/members/{userId} ──
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal User user) {
        log.info("Remove member: team={}, userId={}, by={}", id, userId, user.getEmail());
        teamService.removeMember(id, userId, user);
        return ResponseEntity.noContent().build();
    }

    // ── DELETE /api/teams/{id} ──
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        log.info("Delete team: id={}, by={}", id, user.getEmail());
        teamService.deleteTeam(id, user);
        return ResponseEntity.noContent().build();
    }

    // ── PUT /api/teams/{id} ──
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTeam(
            @PathVariable Long id,
            @RequestBody TeamDTO dto,
            @AuthenticationPrincipal User user) {
        log.info("Update team: id={}, by={}", id, user.getEmail());
        Team team = teamService.updateTeam(id, dto, user);
        return ResponseEntity.ok(toMap(team));
    }

    // ── Helper: serialize Team → Map with activeTaskCount + managerName ──
    private Map<String, Object> toMap(Team team) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",          team.getId());
        map.put("name",        team.getName());
        map.put("description", team.getDescription());
        map.put("createdAt",   team.getCreatedAt());

        // Manager name (safe — avoids lazy load issues)
        if (team.getManager() != null) {
            map.put("managerId",   team.getManager().getId());
            map.put("managerName", team.getManager().getFullName());
        } else {
            map.put("managerId",   null);
            map.put("managerName", "No manager");
        }

        // Members list
        map.put("members", team.getMembers());

        // Active task count — tasks linked to this team that aren't DONE
        long activeTaskCount = taskRepository.findByTeamId(team.getId())
                .stream()
                .filter(t -> !"DONE".equals(t.getStatus() != null ? t.getStatus().name() : ""))
                .count();
        map.put("activeTaskCount", activeTaskCount);

        return map;
    }
}