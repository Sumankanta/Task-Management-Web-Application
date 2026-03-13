package com.infy.tmwa.service;

import com.infy.tmwa.dto.TeamDTO;
import com.infy.tmwa.entity.*;
import com.infy.tmwa.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository       teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository       userRepository;

    // ── CREATE TEAM — Admin/Manager only ──
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Team createTeam(TeamDTO dto, User manager) {
        Team team = Team.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .manager(manager)
                .build();

        Team saved = teamRepository.save(team);
        log.info("Team '{}' created by {}", saved.getName(), manager.getEmail());

        // Auto-add manager as first member
        TeamMember membership = TeamMember.builder()
                .team(saved)
                .user(manager)
                .build();
        teamMemberRepository.save(membership);

        return saved;
    }

    // ── GET TEAMS — scoped by role ──
    // Admin sees all, Manager sees own, Member/Viewer see teams they belong to
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','MEMBER','VIEWER')")
    public List<Team> getTeams(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return teamRepository.findAll();
        }
        if (user.getRole() == UserRole.MANAGER) {
            return teamRepository.findByManager(user);
        }
        // Member/Viewer — return teams they belong to
        return teamMemberRepository.findByUser(user)
                .stream()
                .map(TeamMember::getTeam)
                .distinct()
                .toList();
    }

    // ── GET TEAM BY ID ──
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','MEMBER','VIEWER')")
    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found: " + id));
    }

    // ── ADD MEMBER — Admin/Manager (own team) only ──
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public TeamMember addMember(Long teamId, Long userId, User requester) {
        Team team = findTeam(teamId);
        checkTeamAccess(team, requester);

        User userToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (teamMemberRepository.existsByTeamAndUser(team, userToAdd)) {
            throw new RuntimeException("User is already a member of this team");
        }

        TeamMember member = TeamMember.builder()
                .team(team)
                .user(userToAdd)
                .build();

        TeamMember saved = teamMemberRepository.save(member);
        log.info("User {} added to team {} by {}", userId, teamId, requester.getEmail());
        return saved;
    }

    // ── REMOVE MEMBER — Admin/Manager (own team) only ──
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public void removeMember(Long teamId, Long userId, User requester) {
        Team team = findTeam(teamId);
        checkTeamAccess(team, requester);

        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        teamMemberRepository.deleteByTeamAndUser(team, userToRemove);
        log.info("User {} removed from team {} by {}", userId, teamId, requester.getEmail());
    }

    // ── DELETE TEAM — Admin/Manager (own team) only ──
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public void deleteTeam(Long teamId, User requester) {
        Team team = findTeam(teamId);
        checkTeamAccess(team, requester);
        teamRepository.delete(team);
        log.info("Team {} deleted by {}", teamId, requester.getEmail());
    }

    // ── Helpers ──

    private Team findTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found: " + id));
    }

    // Admin can access any team, Manager can only access their own
    private void checkTeamAccess(Team team, User user) {
        if (user.getRole() == UserRole.ADMIN) return;
        if (!team.getManager().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: you do not manage this team");
        }
    }
}