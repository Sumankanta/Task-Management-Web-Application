// src/app/services/analytics.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Matches TaskSummaryDTO.java exactly (flat structure — no nested objects)
export interface TaskSummary {
  totalTasks:     number;

  // byStatus — flat fields
  todo:           number;
  inProgress:     number;
  done:           number;

  // byPriority — flat fields
  high:           number;
  medium:         number;
  low:            number;

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
