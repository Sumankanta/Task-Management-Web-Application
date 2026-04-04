import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth';
import { TeamService } from '../services/team';
import { ToastService } from '../services/toast';
import { HasRoleDirective } from '../shared/directives/has-role.directive';
import { TaskService } from '../services/task';

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
  teamTasks: any[] = [];
  teamTasksLoading = false;

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
    private toast: ToastService,
    private taskService: TaskService   // ← was missing from constructor
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
        totalTeams:   this.teams.length,
        totalMembers: this.totalMembers,
        activeTasks:  this.activeTasks
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

  // ── Member field helpers ─────────────────────────────────────────────────

  getMemberId(member: any): any {
    return member?.user?.id ?? member?.userId ?? member?.id ?? Math.random();
  }

  getMemberUserId(member: any): number {
    const id = member?.user?.id ?? member?.userId ?? member?.id;
    if (!id) console.error('getMemberUserId: could not resolve userId from member', member);
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

  getMembersList(team: any): any[] {
    return team?.members ?? [];
  }

  getDetailMembers(): any[] {
    return this.teamDetail?.members ?? [];
  }

  // ── Utilities ────────────────────────────────────────────────────────────

  getInitials(name: string): string {
    return name?.split(' ').map((n: string) => n[0]).slice(0, 2).join('').toUpperCase() || '?';
  }

  getAvatarColor(name: string): string {
    const colors = ['#58a6ff', '#3fb950', '#bc8cff', '#d29922', '#f78166', '#39d353', '#79c0ff', '#ffa657'];
    let hash = 0;
    for (const c of name || '') hash = c.charCodeAt(0) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
  }

  getMemberRoleClass(role: string): string {
    const map: Record<string, string> = {
      ADMIN: 'role-admin', MANAGER: 'role-manager', MEMBER: 'role-member', VIEWER: 'role-viewer'
    };
    return map[role] || 'role-member';
  }

  // ── Task helpers ─────────────────────────────────────────────────────────

  getTaskStatusClass(status: string): string {
    const map: Record<string, string> = {
      TODO: 'status-todo', IN_PROGRESS: 'status-progress', DONE: 'status-done'
    };
    return map[status] || 'status-todo';
  }

  getTaskStatusLabel(status: string): string {
    const map: Record<string, string> = {
      TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Done'
    };
    return map[status] || status;
  }

  getPriorityClass(priority: string): string {
    const map: Record<string, string> = {
      HIGH: 'priority-high', MEDIUM: 'priority-medium', LOW: 'priority-low'
    };
    return map[priority] || 'priority-medium';
  }

  // ── Team view / detail ───────────────────────────────────────────────────

  viewTeam(team: any) {
    if (this.selectedTeam?.id === team.id) {
      this.selectedTeam     = null;
      this.teamDetail       = null;
      this.teamTasks        = [];
      return;
    }
    this.selectedTeam     = team;
    this.detailLoading    = true;
    this.teamTasks        = [];
    this.teamTasksLoading = true;

    // Load members
    this.teamService.getTeam(team.id).subscribe({
      next: detail => {
        this.teamDetail    = detail;
        this.detailLoading = false;
        const members = this.getDetailMembers();
        if (members.length > 0) console.log('[Teams] First member:', members[0]);
      },
      error: () => {
        this.toast.show('Failed to load team details', 'error');
        this.detailLoading = false;
      }
    });

    // Load tasks
    this.taskService.getTasksByTeam(team.id).subscribe({
      next: tasks => {
        this.teamTasks        = tasks;
        this.teamTasksLoading = false;
      },
      error: () => { this.teamTasksLoading = false; }
    });
  }

  // ── Edit ─────────────────────────────────────────────────────────────────

  openEdit(team: any) {
    this.editTeamData  = { id: team.id, name: team.name, description: team.description || '' };
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

  // ── Delete ───────────────────────────────────────────────────────────────

  confirmDelete(id: number) {
    this.deleteTargetId    = id;
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
          this.teamDetail   = null;
          this.teamTasks    = [];
        }
        this.deleteTargetId = null;
        this.loadTeams();
      },
      error: () => this.toast.show('Delete failed', 'error')
    });
  }

  // ── Members ──────────────────────────────────────────────────────────────

  searchMembers() {
    const q = this.memberSearch.toLowerCase();
    this.filteredUsers = q
      ? this.allUsers.filter(u =>
          u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q))
      : [];
  }

  addMemberToList(user: any) {
    if (!this.selectedMembers.find(m => m.id === user.id)) {
      this.selectedMembers = [...this.selectedMembers, user];
    }
    this.memberSearch  = '';
    this.filteredUsers = [];
  }

  removeMemberFromList(userId: number) {
    this.selectedMembers = this.selectedMembers.filter(m => m.id !== userId);
  }

  createTeam() {
    if (!this.newTeam.name.trim()) { this.toast.show('Team name is required', 'warning'); return; }
    this.createLoading = true;
    this.teamService.createTeam({
      name:        this.newTeam.name,
      description: this.newTeam.description,
      memberIds:   this.selectedMembers.map(m => m.id)
    }).subscribe({
      next: () => {
        this.toast.show('Team created!', 'success');
        this.showCreateModal = false;
        this.newTeam         = { name: '', description: '' };
        this.selectedMembers = [];
        this.createLoading   = false;
        this.loadTeams();
      },
      error: () => { this.toast.show('Failed to create team', 'error'); this.createLoading = false; }
    });
  }

  addMember(teamId: number, user: any) {
    this.teamService.addMember(teamId, { userId: user.id }).subscribe({
      next: () => {
        this.toast.show(`${user.fullName} joined team`, 'success');
        this.showAddMemberSearch = false;
        this.memberSearch        = '';
        this.filteredUsers       = [];
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
        this.teamService.getTeam(teamId).subscribe({
          next: detail => { this.teamDetail = detail; },
          error: () => {}
        });
        this.loadTeams();
      },
      error: () => this.toast.show('Failed to remove member', 'error')
    });
  }
}
