import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth';
import { HasRoleDirective } from '../directives/has-role.directive';
import { ThemeService } from '../../services/theme';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, HasRoleDirective],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css']
})
export class NavbarComponent implements OnInit {
  menuOpen = false;
  dropdownOpen = false;
  currentUser: any = null;

  constructor(private router: Router, private auth: AuthService, public themeService: ThemeService) {}

  ngOnInit() {
    this.currentUser = this.auth.getCurrentUser();
  }

  get initials(): string {
    return this.currentUser?.fullName?.split(' ').map((n: string) => n[0]).slice(0, 2).join('').toUpperCase() || '?';
  }

  get avatarColor(): string {
    const name = this.currentUser?.fullName || '';
    const colors = ['#6366f1', '#10b981', '#f59e0b', '#3b82f6', '#8b5cf6', '#ec4899'];
    let hash = 0;
    for (const c of name) hash = c.charCodeAt(0) + ((hash << 5) - hash);
    return this.currentUser?.avatarColor || colors[Math.abs(hash) % colors.length];
  }

  getRoleBadgeClass(role: string): string {
    const map: Record<string, string> = {
      ADMIN: 'role-admin', MANAGER: 'role-manager', MEMBER: 'role-member', VIEWER: 'role-viewer'
    };
    return map[role] || 'role-member';
  }

  toggleMenu() { this.menuOpen = !this.menuOpen; }
  toggleDropdown(event?: Event) {
    if (event) event.stopPropagation();
    this.dropdownOpen = !this.dropdownOpen;
  }

  closeDropdown() {
    setTimeout(() => this.dropdownOpen = false, 150);
  }

  goToSettings() {
    this.dropdownOpen = false;
    this.router.navigate(['/settings']);
  }

  logout() {
    this.menuOpen = false;
    this.dropdownOpen = false;
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  toggleTheme() {
    const current = this.themeService.currentTheme();
    if (current === 'LIGHT') this.themeService.setTheme('DARK');
    else this.themeService.setTheme('LIGHT');
  }
}
