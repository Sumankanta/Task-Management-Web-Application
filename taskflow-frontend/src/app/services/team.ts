import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

const BASE = 'http://localhost:8081/api';

@Injectable({ providedIn: 'root' })
export class TeamService {
  constructor(private http: HttpClient) {}

  getTeams() {
    return this.http.get<any[]>(`${BASE}/teams`);
  }

  getTeam(id: number) {
    return this.http.get<any>(`${BASE}/teams/${id}`);
  }

  createTeam(data: any) {
    return this.http.post<any>(`${BASE}/teams`, data);
  }

  updateTeam(id: number, data: any) {
    return this.http.put<any>(`${BASE}/teams/${id}`, data);
  }

  deleteTeam(id: number) {
    return this.http.delete(`${BASE}/teams/${id}`);
  }

  addMember(teamId: number, data: { userId: number; role?: string }) {
    return this.http.post<any>(`${BASE}/teams/${teamId}/members`, data);
  }

  removeMember(teamId: number, userId: number) {
    return this.http.delete(`${BASE}/teams/${teamId}/members/${userId}`);
  }
}
