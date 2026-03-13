package com.infy.tmwa.repository;

import com.infy.tmwa.entity.Team;
import com.infy.tmwa.entity.TeamMember;
import com.infy.tmwa.entity.TeamMemberId;
import com.infy.tmwa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {

    // ── All teams a user belongs to ──
    List<TeamMember> findByUser(User user);

    // ── All members of a team ──
    List<TeamMember> findByTeam(Team team);

    // ── Check membership ──
    boolean existsByTeamAndUser(Team team, User user);

    // ── Remove a user from a team ──
    void deleteByTeamAndUser(Team team, User user);
}