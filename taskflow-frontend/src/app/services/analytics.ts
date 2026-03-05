// src/app/services/analytics.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TaskSummary {
  totalTasks:     number;
  byStatus:       { todo: number; inProgress: number; done: number };
  byPriority:     { high: number; medium: number; low: number };
  completionRate: number;
  overdueCount:   number;
  tasksThisWeek:  number;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private base = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  getSummary(): Observable<TaskSummary> {
    return this.http.get<TaskSummary>(`${this.base}/tasks/summary`);
  }
}
