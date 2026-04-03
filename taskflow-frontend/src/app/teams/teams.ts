import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth';
import { TeamService } from '../services/team';
import { ToastService } from '../services/toast';
import { HasRoleDirective } from '../shared/directives/has-role.directive';

@Component({
  selector: 'app-teams',
  standalone: true,
  imports: [CommonModule, FormsModule, HasRoleDirective],
  templateUrl: './teams.html',
  styleUrls: ['./teams.css']
})
export class TeamsComponent implements OnInit {
  teams: any[] = [];
  allUsers: any[] = [];
  loading = false;

  // Detail panel
  selectedTeam: any = null;
  teamDetail: any = null;
  detailLoading = false;

  // Create modal
  showCreateModal = false;
  newTeam = { name: '', description: '' };
  memberSearch = '';
  filteredUsers: any[] = [];
  selectedMembers: any[] = [];
  createLoading = false;

  // Edit modal
  showEditModal = false;
  editTeamData: any = { name: '', description: '' };

  // Confirm delete
  showDeleteConfirm = false;
  deleteTargetId: number | null = null;
  showAddMemberSearch = false;

  // Analytics
  showAnalytics = false;
  sortByPriority = false;
  analyticsLoaded = false;
  summary: any = null;

  constructor(
    private teamService: TeamService,
    private auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.loadTeams();
    this.auth.getUsers().subscribe({
      next: users => (this.allUsers = users),
      error: err => {
        console.warn('Could not load users list:', err);
        this.allUsers = [];
      }
    });
  }

  loadTeams() {
    this.loading = true;
    this.teamService.getTeams().subscribe({
      next: teams => {
        this.teams = teams;
        this.loading = false;
        if (this.showAnalytics) this.reloadAnalytics();
      },
      error: () => {
        this.toast.show('Failed to load teams', 'error');
        this.loading = false;
      }
    });
  }

  toggleAnalytics() {
    this.showAnalytics = !this.showAnalytics;
    if (this.showAnalytics) this.reloadAnalytics();
  }

  reloadAnalytics() {
    this.analyticsLoaded = false;
    setTimeout(() => {
      this.summary = {
        totalTeams: this.teams.length,
        totalMembers: this.totalMembers,
        activeTasks: this.activeTasks
      };
      this.analyticsLoaded = true;
    }, 600);
  }

  toggleSort() { this.sortByPriority = !this.sortByPriority; }

  get sortedTeams() {
    if (!this.sortByPriority) return this.teams;
    return [...this.teams].sort((a, b) => (b.activeTaskCount || 0) - (a.activeTaskCount || 0));
  }

  get totalMembers(): number {
    return this.teams.reduce((sum, t) => sum + (t.memberCount || t.members?.length || 0), 0);
  }

  get activeTasks(): number {
    return this.teams.reduce((sum, t) => sum + (t.activeTaskCount || 0), 0);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // DEFENSIVE MEMBER FIELD HELPERS
  //
  // The backend may return members in one of these shapes:
  //   Shape A (flat DTO):   { id, fullName, email, role, ... }
  //   Shape B (TeamMember): { id, user: { id, fullName, email }, role, ... }
  //
  // These helpers normalise both shapes so the template never sees `undefined`.
  // ─────────────────────────────────────────────────────────────────────────

  /** The unique key to track by — prefer user.id over TeamMember.id */
  getMemberId(member: any): any {
    return member?.user?.id ?? member?.userId ?? member?.id ?? Math.random();
  }

  /** The user's database id to pass to removeMember() */
  getMemberUserId(member: any): number {
    const id = member?.user?.id ?? member?.userId ?? member?.id;
    if (!id) {
      console.error('getMemberUserId: could not resolve userId from member', member);
    }
    return id;
  }

  getMemberName(member: any): string {
    return member?.user?.fullName ?? member?.fullName ?? member?.user?.name ?? member?.name ?? 'Unknown';
  }

  getMemberEmail(member: any): string {
    return member?.user?.email ?? member?.email ?? '';
  }

  getMemberRole(member: any): string {
    return member?.role ?? member?.user?.role ?? '';
  }

  /** Returns the members array from a team card (list view) */
  getMembersList(team: any): any[] {
    return team?.members ?? [];
  }

  /** Returns the members array from the detail panel */
  getDetailMembers(): any[] {
    // teamDetail may be the Team object itself, or { members: [...] }
    return this.teamDetail?.members ?? [];
  }

  // ─────────────────────────────────────────────────────────────────────────

  getInitials(name: string): string {
    return name?.split(' ').map((n: string) => n[0]).slice(0, 2).join('').toUpperCase() || '?';
  }

  getAvatarColor(name: string): string {
    const colors = [
      '#58a6ff', '#3fb950', '#bc8cff', '#d29922',
      '#f78166', '#39d353', '#79c0ff', '#ffa657'
    ];
    let hash = 0;
    for (const c of name || '') hash = c.charCodeAt(0) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
  }

  viewTeam(team: any) {
    if (this.selectedTeam?.id === team.id) {
      this.selectedTeam = null;
      this.teamDetail = null;
      return;
    }
    this.selectedTeam = team;
    this.detailLoading = true;
    this.teamService.getTeam(team.id).subscribe({
      next: detail => {
        this.teamDetail = detail;
        this.detailLoading = false;

        // Debug: log the first member so you can see the exact shape
        const members = this.getDetailMembers();
        if (members.length > 0) {
          console.log('[Teams] First member object:', members[0]);
        }
      },
      error: () => {
        this.toast.show('Failed to load team details', 'error');
        this.detailLoading = false;
      }
    });
  }

  openEdit(team: any) {
    this.editTeamData = { id: team.id, name: team.name, description: team.description || '' };
    this.showEditModal = true;
  }

  saveEdit() {
    this.teamService.updateTeam(this.editTeamData.id, this.editTeamData).subscribe({
      next: () => {
        this.toast.show('Team updated', 'success');
        this.showEditModal = false;
        this.loadTeams();
      },
      error: () => this.toast.show('Update failed', 'error')
    });
  }

  confirmDelete(id: number) {
    this.deleteTargetId = id;
    this.showDeleteConfirm = true;
  }

  doDelete() {
    if (!this.deleteTargetId) return;
    this.teamService.deleteTeam(this.deleteTargetId).subscribe({
      next: () => {
        this.toast.show('Team deleted', 'success');
        this.showDeleteConfirm = false;
        if (this.selectedTeam?.id === this.deleteTargetId) {
          this.selectedTeam = null;
          this.teamDetail = null;
        }
        this.deleteTargetId = null;
        this.loadTeams();
      },
      error: () => this.toast.show('Delete failed', 'error')
    });
  }

  searchMembers() {
    const q = this.memberSearch.toLowerCase();
    this.filteredUsers = q
      ? this.allUsers.filter(
          u =>
            u.fullName.toLowerCase().includes(q) ||
            u.email.toLowerCase().includes(q)
        )
      : [];
  }

  addMemberToList(user: any) {
    if (!this.selectedMembers.find(m => m.id === user.id)) {
      this.selectedMembers = [...this.selectedMembers, user];
    }
    this.memberSearch = '';
    this.filteredUsers = [];
  }

  removeMemberFromList(userId: number) {
    this.selectedMembers = this.selectedMembers.filter(m => m.id !== userId);
  }

  createTeam() {
    if (!this.newTeam.name.trim()) {
      this.toast.show('Team name is required', 'warning');
      return;
    }
    this.createLoading = true;
    this.teamService
      .createTeam({
        name: this.newTeam.name,
        description: this.newTeam.description,
        memberIds: this.selectedMembers.map(m => m.id)
      })
      .subscribe({
        next: () => {
          this.toast.show('Team created!', 'success');
          this.showCreateModal = false;
          this.newTeam = { name: '', description: '' };
          this.selectedMembers = [];
          this.createLoading = false;
          this.loadTeams();
        },
        error: () => {
          this.toast.show('Failed to create team', 'error');
          this.createLoading = false;
        }
      });
  }

  addMember(teamId: number, user: any) {
    this.teamService.addMember(teamId, { userId: user.id }).subscribe({
      next: () => {
        this.toast.show(`${user.fullName} joined team`, 'success');
        this.showAddMemberSearch = false;
        this.memberSearch = '';
        this.filteredUsers = [];
        // Reload detail without collapsing
        this.teamService.getTeam(teamId).subscribe({
          next: detail => { this.teamDetail = detail; },
          error: () => {}
        });
      },
      error: () => this.toast.show('Failed to add member', 'error')
    });
  }

  removeMember(teamId: number, userId: number) {
    if (!userId) {
      this.toast.show('Cannot remove: user ID is missing', 'error');
      console.error('removeMember called with undefined userId. teamId=', teamId);
      return;
    }
    this.teamService.removeMember(teamId, userId).subscribe({
      next: () => {
        this.toast.show('Member removed', 'success');
        // Reload detail without collapsing
        this.teamService.getTeam(teamId).subscribe({
          next: detail => { this.teamDetail = detail; },
          error: () => {}
        });
        this.loadTeams();
      },
      error: () => this.toast.show('Failed to remove member', 'error')
    });
  }

  getMemberRoleClass(role: string): string {
    const map: Record<string, string> = {
      ADMIN:   'role-admin',
      MANAGER: 'role-manager',
      MEMBER:  'role-member',
      VIEWER:  'role-viewer'
    };
    return map[role] || 'role-member';
  }
}
