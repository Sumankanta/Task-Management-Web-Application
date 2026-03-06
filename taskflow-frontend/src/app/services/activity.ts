// src/app/services/activity.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Matches ActivityLog.java entity field names exactly
export interface ActivityEntry {
  id:          number;
  taskId:      number | null;   // nullable — null when task was deleted
  actor: {                      // nested User object (not actorName string)
    id:        number;
    fullName:  string;
    email:     string;
  };
  actionCode:  string;          // camelCase — matches @Column field in entity
  message:     string;
  createdAt:   string;          // LocalDateTime serialized as ISO string by Jackson
}

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private base = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  getFeed(): Observable<ActivityEntry[]> {
    return this.http.get<ActivityEntry[]>(`${this.base}/activity`);
  }
}
