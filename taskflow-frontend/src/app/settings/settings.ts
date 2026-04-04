import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
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
    avatarColor: '#6366f1'
  };

  bioMax = 200;

  avatarColors = [
    '#6366f1', '#3fb950', '#e3b341', '#58a6ff',
    '#bc8cff', '#ec4899', '#14b8a6', '#f97316'
  ];

  saveProfileLoading = false;
  showDeleteAccountModal = false;
  deleteEmailConfirm = '';

  // ── Security ──
  passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
  passwordStrength = 0;
  passwordError = '';
  sessions: any[] = [];
  sessionsLoading = false;
  showCurrentPw = false;
  showNewPw = false;
  showConfirmPw = false;

  // ── Theme ──
  // Pending selection (not applied until Save is clicked)
  pendingTheme: ThemeMode | null = null;

  themes: { value: ThemeMode; label: string }[] = [
    { value: 'LIGHT', label: 'Light mode' },
    { value: 'DARK', label: 'Dark mode' },
    { value: 'SYSTEM', label: 'Auto' }
  ];

  // ── Notifications ──
  notifPrefs!: NotificationPreferences;
  private notifDebounce: any;

  // In-app notification items (mock — replace with real data from backend)
  notifItems: any[] = [];
  notifFilter: 'ALL' | 'UNREAD' | 'READ' = 'ALL';

  notifRows = [
    { key: 'taskAssigned', label: 'Task assigned to me', desc: 'Show a toast when someone assigns a task to you', color: '#58a6ff', iconBg: '#0d2137' },
    { key: 'commentOnTask', label: 'Comment on my task', desc: 'Notify when someone comments on a task you own', color: '#3fb950', iconBg: '#0d2a14' },
    { key: 'subtaskCompleted', label: 'Subtask completed', desc: 'When a subtask on your task is marked done', color: '#bc8cff', iconBg: '#1a0f2e' },
    { key: 'taskOverdue', label: 'Task overdue', desc: 'Banner when any of your tasks become overdue', color: '#f85149', iconBg: '#2d0f0f' },
    { key: 'teamUpdates', label: 'Team updates', desc: 'Added to or removed from a team', color: '#e3b341', iconBg: '#2d1f07' },
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
    this.currentUser = this.auth.getCurrentUser();

    // ── FIX: always seed profileForm from currentUser so avatar is reactive ──
    this.profileForm.fullName = this.currentUser?.fullName || '';
    this.profileForm.email = this.currentUser?.email || '';
    this.profileForm.bio = this.currentUser?.bio || '';
    this.profileForm.avatarColor = this.currentUser?.avatarColor || '#6366f1';

    this.route.queryParams.subscribe(params => {
      this.activeTab = params['tab'] || 'profile';
      if (this.activeTab === 'security') this.loadSessions();
      if (this.activeTab === 'team') this.loadManagedTeams();
      if (this.activeTab === 'notifications') this.loadNotifications();
    });

    this.notifPrefs = { ...this.notifService.prefs() };
    this.auth.getUsers().subscribe({
      next: users => (this.allUsers = users),
      error: () => (this.allUsers = [])
    });
  }

  setTab(tab: string) {
    this.router.navigate([], { queryParams: { tab }, queryParamsHandling: 'merge' });
  }

  // ── Profile ──
  get initials(): string {
    // Always derived from profileForm so it updates immediately on edit
    return this.profileForm.fullName
      ?.split(' ')
      .map((n: string) => n[0])
      .slice(0, 2)
      .join('')
      .toUpperCase() || '?';
  }

  get bioLength(): number {
    return this.profileForm.bio?.length || 0;
  }

  /** FIX: pick a color → update profileForm immediately; avatar re-renders reactively */
  pickAvatarColor(color: string) {
    this.profileForm.avatarColor = color;
  }

  saveProfile() {
    this.saveProfileLoading = true;
    this.auth.updateProfile({
      fullName: this.profileForm.fullName,
      email: this.profileForm.email,
      bio: this.profileForm.bio,
      avatarColor: this.profileForm.avatarColor
    }).subscribe({
      next: (updatedUser?: any) => {
        this.toast.show('Profile saved!', 'success');
        this.saveProfileLoading = false;
        // Refresh currentUser — keep profileForm as source of truth for avatar
        this.currentUser = this.auth.getCurrentUser();
        if (updatedUser?.avatarColor) {
          this.profileForm.avatarColor = updatedUser.avatarColor;
        }
      },
      error: () => {
        this.toast.show('Failed to save profile', 'error');
        this.saveProfileLoading = false;
      }
    });
  }

  deleteAccount() {
    if (this.deleteEmailConfirm !== this.currentUser?.email) {
      this.toast.show('Email does not match', 'error');
      return;
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
    this.passwordError = '';
    if (!this.passwordForm.currentPassword) { this.passwordError = 'Current password is required'; return; }
    if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) { this.passwordError = 'Passwords do not match'; return; }
    if (this.passwordStrength < 2) { this.passwordError = 'Password is too weak'; return; }
    this.auth.changePassword({
      currentPassword: this.passwordForm.currentPassword,
      newPassword: this.passwordForm.newPassword
    }).subscribe({
      next: () => {
        this.toast.show('Password changed!', 'success');
        this.passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
        this.passwordStrength = 0;
        this.passwordError = '';
        this.showCurrentPw = this.showNewPw = this.showConfirmPw = false;
      },
      error: err => {
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
  /** Select a theme card — does NOT apply immediately; waits for Save */
  selectTheme(theme: ThemeMode) {
    this.pendingTheme = theme;
  }

  get selectedThemeForDisplay(): ThemeMode {
    return this.pendingTheme ?? this.themeService.currentTheme();
  }

  saveTheme() {
    if (!this.pendingTheme) return;
    this.themeService.setTheme(this.pendingTheme);
    this.auth.updatePreferences({ theme: this.pendingTheme }).subscribe();
    this.toast.show('Theme updated!', 'success');
    this.pendingTheme = null;
  }

  cancelTheme() {
    this.pendingTheme = null;
  }

  // ── Notifications ──
  loadNotifications() {
    // Seed with mock data — replace with real API call if available
    if (!this.notifItems.length) {
      this.notifItems = [
        { id: 1, title: 'Welcome to TaskFlow!', body: 'Your account has been successfully created', read: false, type: 'success', time: new Date(Date.now() - 3 * 60000) },
        { id: 2, title: 'Get started', body: 'Add your first task to begin managing work', read: false, type: 'info', time: new Date(Date.now() - 8 * 60000) },
      ];
    }
  }

  get filteredNotifItems(): any[] {
    if (this.notifFilter === 'UNREAD') return this.notifItems.filter(n => !n.read);
    if (this.notifFilter === 'READ') return this.notifItems.filter(n => n.read);
    return this.notifItems;
  }

  get unreadCount(): number { return this.notifItems.filter(n => !n.read).length; }

  markRead(item: any) { item.read = true; }
  deleteNotif(item: any) { this.notifItems = this.notifItems.filter(n => n.id !== item.id); }
  markAllRead() { this.notifItems.forEach(n => n.read = true); }
  clearAllNotif() { this.notifItems = []; }

  getNotifIcon(type: string): string {
    const map: Record<string, string> = { success: '✓', info: '!', warning: '⚠', error: '✕' };
    return map[type] || '•';
  }

  getNotifIconColor(type: string): string {
    const map: Record<string, string> = { success: '#3fb950', info: '#e3b341', warning: '#f97316', error: '#f85149' };
    return map[type] || '#7d8590';
  }

  getRelativeTime(date: Date): string {
    const diff = Date.now() - new Date(date).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
  }

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
      next: teams => {
        this.managedTeams = teams;
        this.teamsLoading = false;
        if (teams.length > 0 && teams[0].members?.length > 0) {
          console.log('[Settings] First team member shape:', teams[0].members[0]);
        }
      },
      error: () => { this.toast.show('Failed to load teams', 'error'); this.teamsLoading = false; }
    });
  }

  openTeamCreate() { this.newTeamData = { name: '', description: '', memberIds: [] }; this.showTeamCreateModal = true; }
  createTeam() {
    if (!this.newTeamData.name.trim()) { this.toast.show('Name is required', 'warning'); return; }
    this.teamService.createTeam(this.newTeamData).subscribe({
      next: () => { this.toast.show('Team created!', 'success'); this.showTeamCreateModal = false; this.loadManagedTeams(); },
      error: () => this.toast.show('Failed to create team', 'error')
    });
  }

  openTeamEdit(team: any) { this.editingTeam = { ...team }; this.showTeamEditModal = true; }
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
      next: () => { this.toast.show('Team deleted', 'success'); this.showTeamDeleteConfirm = false; this.loadManagedTeams(); },
      error: () => this.toast.show('Delete failed', 'error')
    });
  }

  searchTeamUsers(teamId: number, query: string) {
    if (!query) { this.teamUserSearch[teamId] = []; return; }
    const q = query.toLowerCase();
    this.teamUserSearch[teamId] = this.allUsers.filter(u => u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q));
  }

  inviteMember(teamId: number, user: any) {
    this.teamService.addMember(teamId, { userId: user.id }).subscribe({
      next: () => { this.toast.show(`${user.fullName} added`, 'success'); this.teamUserSearch[teamId] = []; this.teamInviteUser[teamId] = ''; this.loadManagedTeams(); },
      error: () => this.toast.show('Failed to add member', 'error')
    });
  }

  // ── Member helpers ──
  getMemberId(member: any): any { return member?.user?.id ?? member?.userId ?? member?.id ?? Math.random(); }
  getMemberName(member: any): string { return member?.user?.fullName ?? member?.fullName ?? member?.user?.name ?? member?.name ?? 'Unknown'; }
  getMemberEmail(member: any): string { return member?.user?.email ?? member?.email ?? ''; }
  getMemberRole(member: any): string { return member?.role ?? member?.user?.role ?? ''; }
  getMembersList(team: any): any[] { return team?.members ?? []; }

  getInitials(name: string): string {
    return name?.split(' ').map((n: string) => n[0]).slice(0, 2).join('').toUpperCase() || '?';
  }

  getAvatarColor(name: string): string {
    const colors = ['#6366f1', '#3fb950', '#e3b341', '#58a6ff', '#bc8cff', '#ec4899', '#14b8a6', '#f97316'];
    let h = 0;
    for (const c of name || '') h = c.charCodeAt(0) + ((h << 5) - h);
    return colors[Math.abs(h) % colors.length];
  }

  getRoleBadge(role: string): string {
    const map: Record<string, string> = { ADMIN: 'role-admin', MANAGER: 'role-manager', MEMBER: 'role-member', VIEWER: 'role-viewer' };
    return map[role] || 'role-member';
  }
}
