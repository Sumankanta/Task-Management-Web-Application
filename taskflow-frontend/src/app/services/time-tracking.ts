import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

const BASE = 'http://localhost:8081/api';

@Injectable({ providedIn: 'root' })
export class TimeTrackingService {
  constructor(private http: HttpClient) {}

  startTimer(taskId: number) {
    return this.http.post<any>(`${BASE}/tasks/${taskId}/timer/start`, {});
  }

  stopTimer(taskId: number) {
    return this.http.post<any>(`${BASE}/tasks/${taskId}/timer/stop`, {});
  }

  getTimeLogs(taskId: number) {
    return this.http.get<any[]>(`${BASE}/tasks/${taskId}/time-logs`);
  }

  getTotalTime(taskId: number) {
    return this.http.get<{ totalMinutes: number }>(`${BASE}/tasks/${taskId}/time-logs/total`);
  }

  addManualLog(taskId: number, data: { durationMinutes: number; logDate: string; note?: string }) {
    return this.http.post<any>(`${BASE}/tasks/${taskId}/time-logs`, data);
  }

  deleteLog(logId: number) {
    return this.http.delete(`${BASE}/time-logs/${logId}`);
  }

  formatDuration(minutes: number): string {
    if (!minutes || minutes === 0) return 'No time logged';
    if (minutes < 60) return `${minutes}m`;
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    if (m === 0) return `${h}h`;
    return `${h}h ${m}m`;
  }
}
