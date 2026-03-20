import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

const BASE = 'http://localhost:8081/api';

@Injectable({ providedIn: 'root' })
export class SubtaskService {
  constructor(private http: HttpClient) {}

  getSubtasks(taskId: number) {
    return this.http.get<any[]>(`${BASE}/tasks/${taskId}/subtasks`);
  }

  getSubtaskSummary(taskId: number) {
    return this.http.get<{ total: number; completed: number }>(`${BASE}/tasks/${taskId}/subtasks/summary`);
  }

  createSubtask(taskId: number, data: { title: string; assignedTo?: number }) {
    return this.http.post<any>(`${BASE}/tasks/${taskId}/subtasks`, data);
  }

  toggleSubtask(subtaskId: number) {
    return this.http.patch<any>(`${BASE}/subtasks/${subtaskId}/toggle`, {});
  }

  updateSubtask(subtaskId: number, data: any) {
    return this.http.put<any>(`${BASE}/subtasks/${subtaskId}`, data);
  }

  deleteSubtask(subtaskId: number) {
    return this.http.delete(`${BASE}/subtasks/${subtaskId}`);
  }
}
