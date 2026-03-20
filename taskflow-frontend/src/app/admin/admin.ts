import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../services/admin';
import { ToastService } from '../services/toast';
import { HasRoleDirective } from '../shared/directives/has-role.directive';
import { NavbarComponent } from '../shared/navbar/navbar';


@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, HasRoleDirective],
  templateUrl: './admin.html',
  styleUrls: ['./admin.css']
})
export class AdminComponent implements OnInit {
  users: any[] = [];
  loading = false;
  searchQuery = '';
  roleFilter = 'ALL';

  showDeleteConfirm = false;
  deleteTargetId: number | null = null;
  deleteTargetName = '';

  roles = ['ADMIN', 'MANAGER', 'MEMBER', 'VIEWER'];
  filterTabs = ['ALL', 'ADMIN', 'MANAGER', 'MEMBER', 'VIEWER', 'INACTIVE'];

  constructor(private adminService: AdminService, private toast: ToastService) { }

  ngOnInit() { this.loadUsers(); }

  loadUsers() {
    this.loading = true;
    this.adminService.getUsers().subscribe({
      next: users => { this.users = users; this.loading = false; },
      error: () => { this.toast.show('Failed to load users', 'error'); this.loading = false; }
    });
  }

  get filteredUsers(): any[] {
    return this.users.filter(u => {
      const matchSearch = !this.searchQuery ||
        u.fullName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        u.email.toLowerCase().includes(this.searchQuery.toLowerCase());

      const matchRole = this.roleFilter === 'ALL' ? true
        : this.roleFilter === 'INACTIVE' ? !u.isActive
          : u.role === this.roleFilter && u.isActive;

      return matchSearch && matchRole;
    });
  }

  get stats() {
    return {
      total: this.users.length,
      admins: this.users.filter(u => u.role === 'ADMIN').length,
      managers: this.users.filter(u => u.role === 'MANAGER').length,
      deactivated: this.users.filter(u => !u.isActive).length
    };
  }

  changeRole(userId: number, role: string) {
    this.adminService.updateRole(userId, role).subscribe({
      next: () => this.toast.show('Role updated — takes effect on next login', 'success'),
      error: () => this.toast.show('Failed to update role', 'error')
    });
  }

  toggleStatus(user: any) {
    const newStatus = !user.isActive;
    this.adminService.updateStatus(user.id, newStatus).subscribe({
      next: () => {
        user.isActive = newStatus;
        this.toast.show(`User ${newStatus ? 'activated' : 'deactivated'}`, 'success');
      },
      error: () => this.toast.show('Failed to update status', 'error')
    });
  }

  confirmDelete(user: any) {
    this.deleteTargetId = user.id;
    this.deleteTargetName = user.fullName;
    this.showDeleteConfirm = true;
  }

  doDelete() {
    if (!this.deleteTargetId) return;
    this.adminService.deleteUser(this.deleteTargetId).subscribe({
      next: () => {
        this.toast.show('User deleted', 'success');
        this.users = this.users.filter(u => u.id !== this.deleteTargetId);
        this.showDeleteConfirm = false;
        this.deleteTargetId = null;
      },
      error: () => this.toast.show('Failed to delete user', 'error')
    });
  }

  getInitials(name: string): string {
    return name?.split(' ').map((n: string) => n[0]).slice(0, 2).join('').toUpperCase() || '?';
  }

  getAvatarColor(name: string): string {
    const colors = ['#6366f1', '#10b981', '#f59e0b', '#3b82f6', '#8b5cf6', '#ec4899'];
    let hash = 0;
    for (const c of name || '') hash = c.charCodeAt(0) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
  }

  getRoleBadgeClass(role: string): string {
    const map: Record<string, string> = {
      ADMIN: 'role-admin', MANAGER: 'role-manager', MEMBER: 'role-member', VIEWER: 'role-viewer'
    };
    return map[role] || 'role-member';
  }
}
