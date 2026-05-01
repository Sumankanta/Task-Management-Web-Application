import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';

const BASE = 'http://localhost:8081/api/notifications';

export interface NotificationPreferences {
  taskAssigned: boolean;
  commentOnTask: boolean;
  subtaskCompleted: boolean;
  taskOverdue: boolean;
  teamUpdates: boolean;
}

export interface AppNotification {
time: string;
read: any;
  id: number;
  title: string;
  body: string;
  type: string;         // TASK_ASSIGNED | TEAM_ADDED | TEAM_REMOVED | GENERAL
  isRead: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {

  // ── In-app notification preferences (kept as signals for reactivity) ──
  prefs = signal<NotificationPreferences>({
    taskAssigned:     true,
    commentOnTask:    true,
    subtaskCompleted: true,
    taskOverdue:      true,
    teamUpdates:      true
  });

  constructor(private http: HttpClient) {}

  loadPrefs(data: Partial<NotificationPreferences>) {
    this.prefs.set({ ...this.prefs(), ...data });
  }

  canNotify(type: keyof NotificationPreferences): boolean {
    return this.prefs()[type];
  }

  // ── API calls ──────────────────────────────────────────────────

  /** Fetch all notifications for the logged-in user */
  getAll() {
    return this.http.get<AppNotification[]>(BASE);
  }

  /** Get unread count */
  getUnreadCount() {
    return this.http.get<{ count: number }>(`${BASE}/unread-count`);
  }

  /** Mark a single notification as read */
  markRead(id: number) {
    return this.http.patch<void>(`${BASE}/${id}/read`, {});
  }

  /** Mark all notifications as read */
  markAllRead() {
    return this.http.patch<void>(`${BASE}/read-all`, {});
  }

  /** Delete a single notification */
  deleteOne(id: number) {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  /** Clear all notifications */
  clearAll() {
    return this.http.delete<void>(BASE);
  }
}
