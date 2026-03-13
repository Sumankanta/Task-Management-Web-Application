package com.infy.tmwa.entity;

import java.io.Serializable;
import java.util.Objects;

// ── Composite PK class for TeamMember (team_id + user_id) ──
public class TeamMemberId implements Serializable {

    private Team team;
    private User user;

    public TeamMemberId() {}

    public TeamMemberId(Team team, User user) {
        this.team = team;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeamMemberId)) return false;
        TeamMemberId that = (TeamMemberId) o;
        return Objects.equals(team, that.team) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(team, user);
    }
}