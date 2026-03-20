import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

const BASE = 'http://localhost:8081/api/admin';

@Injectable({ providedIn: 'root' })
export class AdminService {
  constructor(private http: HttpClient) {}

  getUsers() {
    return this.http.get<any[]>(`${BASE}/users`);
  }

  updateRole(userId: number, role: string) {
    return this.http.patch(`${BASE}/users/${userId}/role`, { role });
  }

  updateStatus(userId: number, isActive: boolean) {
    return this.http.patch(`${BASE}/users/${userId}/status`, { isActive });
  }

  deleteUser(userId: number) {
    return this.http.delete(`${BASE}/users/${userId}`);
  }
}
