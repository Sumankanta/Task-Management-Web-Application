package com.infy.tmwa.controller;

import com.infy.tmwa.dto.TeamDTO;
import com.infy.tmwa.entity.Team;
import com.infy.tmwa.entity.TeamMember;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class TeamController {

    private final TeamService teamService;

    // ── POST /api/teams ── Admin/Manager only
    @PostMapping
    public ResponseEntity<Team> createTeam(
            @RequestBody TeamDTO dto,
            @AuthenticationPrincipal User user) {

        log.info("Create team: name='{}', user={}", dto.getName(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createTeam(dto, user));
    }

    // ── GET /api/teams ── Scoped by role (Admin=all, Manager=own, Member/Viewer=joined)
    @GetMapping
    public ResponseEntity<List<Team>> getTeams(
            @AuthenticationPrincipal User user) {

        log.info("Get teams for user={}, role={}", user.getEmail(), user.getRole());
        return ResponseEntity.ok(teamService.getTeams(user));
    }

    // ── GET /api/teams/{id} ── Team detail with members
    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    // ── POST /api/teams/{id}/members ── Add user to team
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

    // ── DELETE /api/teams/{id}/members/{userId} ── Remove user from team
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal User user) {

        log.info("Remove member: team={}, userId={}, by={}", id, userId, user.getEmail());
        teamService.removeMember(id, userId, user);
        return ResponseEntity.noContent().build();
    }

    // ── DELETE /api/teams/{id} ── Delete team
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("Delete team: id={}, by={}", id, user.getEmail());
        teamService.deleteTeam(id, user);
        return ResponseEntity.noContent().build();
    }
}