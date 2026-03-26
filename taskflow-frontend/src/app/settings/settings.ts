import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth';
import { NotificationPreferences, NotificationService } from '../services/notification';
import { TeamService } from '../services/team';
import { ThemeMode, ThemeService } from '../services/theme';
import { ToastService } from '../services/toast';
import { HasRoleDirective } from '../shared/directives/has-role.directive';


@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, HasRoleDirective],
  templateUrl: './settings.html',
  styleUrls: ['./settings.css']
})
export class SettingsComponent implements OnInit {
  activeTab = 'profile';
  currentUser: any = null;

  // ── Profile ──
  profileForm = {
    fullName: '',
    email: '',
    bio: '',
    avatarColor: '#6366f1',
    currentPasswordForEmail: ''
  };
  bioMax = 200;
  avatarColors = ['#6366f1', '#10b981', '#f59e0b', '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316'];
  saveProfileLoading = false;
  showDeleteAccountModal = false;
  deleteEmailConfirm = '';

  // ── Security ──
  passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
  passwordStrength = 0;
  passwordError = '';
  sessions: any[] = [];
  sessionsLoading = false;

  // ── Theme ──
  themes: { value: ThemeMode; label: string; icon: string }[] = [
    { value: 'LIGHT', label: 'Light', icon: '☀️' },
    { value: 'DARK', label: 'Dark', icon: '🌙' },
    { value: 'SYSTEM', label: 'System', icon: '🖥️' }
  ];

  // ── Notifications ──
  notifPrefs!: NotificationPreferences;
  private notifDebounce: any;
  notifRows = [
    { key: 'taskAssigned', label: 'Task assigned to me', desc: 'Show a toast when someone assigns a task to me', color: '#3b82f6', icon: '📋' },
    { key: 'commentOnTask', label: 'Comment on my task', desc: 'Notify when someone comments on a task I own', color: '#10b981', icon: '💬' },
    { key: 'subtaskCompleted', label: 'Subtask completed', desc: 'Notify when a subtask on my task is marked done', color: '#8b5cf6', icon: '✅' },
    { key: 'taskOverdue', label: 'Task overdue', desc: 'Show a banner when any of my tasks become overdue', color: '#ef4444', icon: '⏰' },
    { key: 'teamUpdates', label: 'Team updates', desc: 'Notify when I am added to or removed from a team', color: '#f59e0b', icon: '👥' },
  ];

  // ── Team Settings ──
  managedTeams: any[] = [];
  teamsLoading = false;
  editingTeam: any = null;
  showTeamCreateModal = false;
  newTeamData = { name: '', description: '', memberIds: [] as number[] };
  showTeamEditModal = false;
  showTeamDeleteConfirm = false;
  deleteTeamId: number | null = null;
  teamInviteUser: Record<number, string> = {};
  teamUserSearch: Record<number, any[]> = {};
  allUsers: any[] = [];

  constructor(
    private auth: AuthService,
    public themeService: ThemeService,
    private toast: ToastService,
    private teamService: TeamService,
    private notifService: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) { }

  ngOnInit() {
    // Initialize currentUser and profileForm here, after DI is ready
    this.currentUser = this.auth.getCurrentUser();
    this.profileForm.fullName = this.currentUser?.fullName || '';
    this.profileForm.email = this.currentUser?.email || '';
    if (this.currentUser?.avatarColor) {
      this.profileForm.avatarColor = this.currentUser.avatarColor;
    }

    this.route.queryParams.subscribe(params => {
      this.activeTab = params['tab'] || 'profile';
      if (this.activeTab === 'security') this.loadSessions();
      if (this.activeTab === 'team') this.loadManagedTeams();
    });

    // Load notification prefs from stored user or default
    this.notifPrefs = { ...this.notifService.prefs() };

    this.auth.getUsers().subscribe(users => this.allUsers = users);
  }

  setTab(tab: string) {
    this.router.navigate([], { queryParams: { tab }, queryParamsHandling: 'merge' });
  }

  // ── Profile ──
  get initials(): string {
    return this.profileForm.fullName?.split(' ').map((n: string) => n[0]).slice(0, 2).join('').toUpperCase() || '?';
  }

  get bioLength(): number { return this.profileForm.bio?.length || 0; }

  saveProfile() {
    this.saveProfileLoading = true;
    this.auth.updateProfile({
      fullName: this.profileForm.fullName,
      email: this.profileForm.email,
      bio: this.profileForm.bio,
      avatarColor: this.profileForm.avatarColor
    }).subscribe({
      next: () => { this.toast.show('Profile saved!', 'success'); this.saveProfileLoading = false; this.currentUser = this.auth.getCurrentUser(); },
      error: () => { this.toast.show('Failed to save profile', 'error'); this.saveProfileLoading = false; }
    });
  }

  deleteAccount() {
    if (this.deleteEmailConfirm !== this.currentUser?.email) {
      this.toast.show('Email does not match', 'error'); return;
    }
    this.auth.deleteAccount().subscribe({
      next: () => { this.auth.logout(); this.router.navigate(['/register']); },
      error: () => this.toast.show('Failed to delete account', 'error')
    });
  }

  // ── Security ──
  onNewPasswordChange() {
    const p = this.passwordForm.newPassword;
    let score = 0;
    if (p.length >= 8) score++;
    if (/[A-Z]/.test(p)) score++;
    if (/[0-9!@#$%^&*]/.test(p)) score++;
    this.passwordStrength = score;
  }

  changePassword() {
    if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
      this.passwordError = 'Passwords do not match'; return;
    }
    this.auth.changePassword({
      currentPassword: this.passwordForm.currentPassword,
      newPassword: this.passwordForm.newPassword
    }).subscribe({
      next: () => {
        this.toast.show('Password changed!', 'success');
        this.passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
        this.passwordStrength = 0;
        this.passwordError = '';
      },
      error: (err) => {
        this.passwordError = err.status === 400 ? 'Current password is incorrect' : 'Failed to change password';
      }
    });
  }

  loadSessions() {
    this.sessionsLoading = true;
    this.auth.getSessions().subscribe({
      next: s => { this.sessions = s; this.sessionsLoading = false; },
      error: () => { this.toast.show('Failed to load sessions', 'error'); this.sessionsLoading = false; }
    });
  }

  revokeSession(jti: string) {
    this.auth.revokeSession(jti).subscribe({
      next: () => { this.sessions = this.sessions.filter(s => s.jti !== jti); this.toast.show('Session revoked', 'success'); },
      error: () => this.toast.show('Failed to revoke session', 'error')
    });
  }

  revokeAll() {
    this.auth.revokeAllOtherSessions().subscribe({
      next: () => { this.sessions = this.sessions.filter(s => s.current); this.toast.show('All other sessions revoked', 'success'); },
      error: () => this.toast.show('Failed to revoke sessions', 'error')
    });
  }

  // ── Theme ──
  setTheme(theme: ThemeMode) {
    this.themeService.setTheme(theme);
    this.auth.updatePreferences({ theme }).subscribe();
  }

  // ── Notifications ──
  onNotifToggle(key: string) {
    (this.notifPrefs as any)[key] = !(this.notifPrefs as any)[key];
    this.notifService.loadPrefs(this.notifPrefs);
    clearTimeout(this.notifDebounce);
    this.notifDebounce = setTimeout(() => {
      this.auth.updatePreferences({ notifications: this.notifPrefs }).subscribe();
    }, 500);
  }

  // ── Team Settings ──
  loadManagedTeams() {
    this.teamsLoading = true;
    this.teamService.getTeams().subscribe({
      next: teams => { this.managedTeams = teams; this.teamsLoading = false; },
      error: () => { this.toast.show('Failed to load teams', 'error'); this.teamsLoading = false; }
    });
  }

  openTeamCreate() {
    this.newTeamData = { name: '', description: '', memberIds: [] };
    this.showTeamCreateModal = true;
  }

  createTeam() {
    if (!this.newTeamData.name) { this.toast.show('Name is required', 'warning'); return; }
    this.teamService.createTeam(this.newTeamData).subscribe({
      next: () => {
        this.toast.show('Team created!', 'success');
        this.showTeamCreateModal = false;
        this.loadManagedTeams();
      },
      error: () => this.toast.show('Failed to create team', 'error')
    });
  }

  openTeamEdit(team: any) {
    this.editingTeam = { ...team };
    this.showTeamEditModal = true;
  }

  saveTeamEdit() {
    this.teamService.updateTeam(this.editingTeam.id, this.editingTeam).subscribe({
      next: () => { this.toast.show('Team updated', 'success'); this.showTeamEditModal = false; this.loadManagedTeams(); },
      error: () => this.toast.show('Update failed', 'error')
    });
  }

  confirmDeleteTeam(id: number) { this.deleteTeamId = id; this.showTeamDeleteConfirm = true; }

  deleteTeam() {
    if (!this.deleteTeamId) return;
    this.teamService.deleteTeam(this.deleteTeamId).subscribe({
      next: () => {
        this.toast.show('Team deleted', 'success');
        this.showTeamDeleteConfirm = false;
        this.loadManagedTeams();
      },
      error: () => this.toast.show('Delete failed', 'error')
    });
  }

  searchTeamUsers(teamId: number, query: string) {
    if (!query) { this.teamUserSearch[teamId] = []; return; }
    const q = query.toLowerCase();
    this.teamUserSearch[teamId] = this.allUsers.filter(u =>
      u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
    );
  }

  inviteMember(teamId: number, user: any) {
    this.teamService.addMember(teamId, { userId: user.id }).subscribe({
      next: () => { this.toast.show(`${user.fullName} added`, 'success'); this.teamUserSearch[teamId] = []; this.teamInviteUser[teamId] = ''; this.loadManagedTeams(); },
      error: () => this.toast.show('Failed to add member', 'error')
    });
  }

  getInitials(name: string): string {
    return name?.split(' ').map((n: string) => n[0]).slice(0, 2).join('').toUpperCase() || '?';
  }

  getAvatarColor(name: string): string {
    const colors = ['#6366f1', '#10b981', '#f59e0b', '#3b82f6', '#8b5cf6'];
    let h = 0;
    for (const c of name || '') h = c.charCodeAt(0) + ((h << 5) - h);
    return colors[Math.abs(h) % colors.length];
  }

  getRoleBadge(role: string): string {
    const map: Record<string, string> = { ADMIN: 'role-admin', MANAGER: 'role-manager', MEMBER: 'role-member', VIEWER: 'role-viewer' };
    return map[role] || 'role-member';
  }
}
