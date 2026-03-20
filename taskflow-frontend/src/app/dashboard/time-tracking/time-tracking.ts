import { Component, Input, OnInit, OnDestroy, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TimeTrackingService } from '../../services/time-tracking';
import { AuthService } from '../../services/auth';
import { ToastService } from '../../services/toast';

@Component({
  selector: 'app-time-tracking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './time-tracking.html',
  styleUrls: ['./time-tracking.css']
})
export class TimeTrackingComponent implements OnChanges, OnDestroy {
  @Input() taskId!: number;

  timeLogs: any[] = [];
  totalMinutes = 0;
  loading = false;

  // Timer state
  timerRunning = false;
  elapsedSeconds = 0;
  private timerInterval: any = null;
  timerDisplay = '00 : 00 : 00';

  // Manual form
  showManualForm = false;
  manualHours = 0;
  manualMinutes = 0;
  manualDate = new Date().toISOString().split('T')[0];
  manualNote = '';

  currentUser: any;

  constructor(
    private ttService: TimeTrackingService,
    private auth: AuthService,
    private toast: ToastService
  ) {
    this.currentUser = this.auth.getCurrentUser();
  }

  ngOnChanges() {
    if (this.taskId) this.init();
  }

  ngOnDestroy() {
    this.stopInterval();
  }

  init() {
    this.loading = true;
    this.ttService.getTimeLogs(this.taskId).subscribe({
      next: logs => {
        this.timeLogs = logs;
        this.loading = false;
        this.checkActiveTimer(logs);
      },
      error: () => { this.toast.show('Failed to load time logs', 'error'); this.loading = false; }
    });
    this.ttService.getTotalTime(this.taskId).subscribe({
      next: res => this.totalMinutes = res.totalMinutes
    });
  }

  private checkActiveTimer(logs: any[]) {
    const activeLog = logs.find(l => l.startTime && !l.isManual && !l.endTime);
    if (activeLog) {
      const startMs = new Date(activeLog.startTime).getTime();
      this.elapsedSeconds = Math.floor((Date.now() - startMs) / 1000);
      this.timerRunning = true;
      this.startInterval();
    }
  }

  private startInterval() {
    this.stopInterval();
    this.updateDisplay();
    this.timerInterval = setInterval(() => {
      this.elapsedSeconds++;
      this.updateDisplay();
    }, 1000);
  }

  private stopInterval() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  private updateDisplay() {
    const h = Math.floor(this.elapsedSeconds / 3600);
    const m = Math.floor((this.elapsedSeconds % 3600) / 60);
    const s = this.elapsedSeconds % 60;
    this.timerDisplay = `${String(h).padStart(2,'0')} : ${String(m).padStart(2,'0')} : ${String(s).padStart(2,'0')}`;
  }

  startTimer() {
    this.ttService.startTimer(this.taskId).subscribe({
      next: () => {
        this.timerRunning = true;
        this.elapsedSeconds = 0;
        this.startInterval();
        this.toast.show('Timer started', 'success');
      },
      error: (err) => {
        if (err.status === 409) {
          this.toast.show('A timer is already running for this task.', 'warning');
        } else {
          this.toast.show('Failed to start timer', 'error');
        }
      }
    });
  }

  stopTimer() {
    this.ttService.stopTimer(this.taskId).subscribe({
      next: () => {
        this.timerRunning = false;
        this.stopInterval();
        this.elapsedSeconds = 0;
        this.timerDisplay = '00 : 00 : 00';
        this.toast.show('Timer stopped — log saved', 'success');
        this.init();
      },
      error: () => this.toast.show('Failed to stop timer', 'error')
    });
  }

  submitManual() {
    const totalMins = (this.manualHours * 60) + this.manualMinutes;
    if (totalMins <= 0) { this.toast.show('Enter a valid duration', 'warning'); return; }
    this.ttService.addManualLog(this.taskId, {
      durationMinutes: totalMins,
      logDate: this.manualDate,
      note: this.manualNote
    }).subscribe({
      next: () => {
        this.toast.show('Time logged', 'success');
        this.showManualForm = false;
        this.manualHours = 0; this.manualMinutes = 0;
        this.manualNote = '';
        this.init();
      },
      error: () => this.toast.show('Failed to log time', 'error')
    });
  }

  deleteLog(log: any) {
    if (!log.isManual) return;
    this.ttService.deleteLog(log.id).subscribe({
      next: () => {
        this.toast.show('Log deleted', 'success');
        this.timeLogs = this.timeLogs.filter(l => l.id !== log.id);
        this.ttService.getTotalTime(this.taskId).subscribe(r => this.totalMinutes = r.totalMinutes);
      },
      error: (err) => {
        if (err.status === 403) this.toast.show('Cannot delete timer entries', 'warning');
        else this.toast.show('Delete failed', 'error');
      }
    });
  }

  get isViewer(): boolean { return this.currentUser?.role === 'VIEWER'; }
  get totalFormatted(): string { return this.ttService.formatDuration(this.totalMinutes); }

  formatDuration(minutes: number): string { return this.ttService.formatDuration(minutes); }
}
