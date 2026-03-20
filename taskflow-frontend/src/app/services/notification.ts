import { Injectable, signal } from '@angular/core';

export interface NotificationPreferences {
  taskAssigned: boolean;
  commentOnTask: boolean;
  subtaskCompleted: boolean;
  taskOverdue: boolean;
  teamUpdates: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  prefs = signal<NotificationPreferences>({
    taskAssigned: true,
    commentOnTask: true,
    subtaskCompleted: true,
    taskOverdue: true,
    teamUpdates: true
  });

  loadPrefs(data: Partial<NotificationPreferences>) {
    this.prefs.set({ ...this.prefs(), ...data });
  }

  canNotify(type: keyof NotificationPreferences): boolean {
    return this.prefs()[type];
  }
}
