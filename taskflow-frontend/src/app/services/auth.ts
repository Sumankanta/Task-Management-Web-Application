import { Injectable } from '@angular/core'
import { HttpClient } from '@angular/common/http'
import { CurrentUser, Role } from '../models/user';
import { tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  API = 'http://localhost:8081/api/auth'

  constructor(private http: HttpClient) { }

  register(data: any) {
    return this.http.post(`${this.API}/register`, data)
  }

  login(data: any) {
    return this.http.post<{ token: string }>(`${this.API}/login`, data).pipe(
      tap(response => {
        this.saveToken(response.token);
        const payload = this.decodeToken(response.token);
        const user: CurrentUser = {
          id: payload.userId,
          fullName: payload.fullName,
          email: payload.sub,
          role: payload.role as Role,
        };
        localStorage.setItem('currentUser', JSON.stringify(user));
      })
    );
  }

  saveToken(token: string) {
    localStorage.setItem('token', token)
  }

  getToken() {
    return localStorage.getItem('token')
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
  }

  isLoggedIn() {
    return !!this.getToken()
  }

  getCurrentUser(): CurrentUser | null {
    const stored = localStorage.getItem('currentUser');
    if (!stored) return null;
    return JSON.parse(stored) as CurrentUser;
  }

  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user?.role === role;
  }

  hasAnyRole(roles: string[]): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    return roles.includes(user.role);
  }

  getUsers() {
    return this.http.get<any[]>("http://localhost:8081/api/users")
  }

  updateProfile(data: any) {
    return this.http.patch<any>('http://localhost:8081/api/users/me/profile', data).pipe(
      tap(updated => {
        const current = this.getCurrentUser();
        if (current) {
          const merged = { ...current, ...updated };
          localStorage.setItem('currentUser', JSON.stringify(merged));
        }
      })
    );
  }

  changePassword(data: { currentPassword: string; newPassword: string }) {
    return this.http.patch('http://localhost:8081/api/users/me/password', data);
  }

  getSessions() {
    return this.http.get<any[]>('http://localhost:8081/api/users/me/sessions');
  }

  revokeSession(jti: string) {
    return this.http.delete(`http://localhost:8081/api/users/me/sessions/${jti}`);
  }

  revokeAllOtherSessions() {
    return this.http.delete('http://localhost:8081/api/users/me/sessions');
  }

  deleteAccount() {
    return this.http.delete('http://localhost:8081/api/users/me');
  }

  updatePreferences(data: any) {
    return this.http.patch('http://localhost:8081/api/users/me/preferences', data);
  }

  private decodeToken(token: string): any {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload))
    } catch {
      return {};
    }
  }

}
