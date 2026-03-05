// src/app/services/activity.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ActivityEntry {
  id:          number;
  actionCode:  string;
  message:     string;
  createdAt:   string;
  actorName?:  string;
}

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private base = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  getFeed(): Observable<ActivityEntry[]> {
    return this.http.get<ActivityEntry[]>(`${this.base}/activity`);
  }
}
