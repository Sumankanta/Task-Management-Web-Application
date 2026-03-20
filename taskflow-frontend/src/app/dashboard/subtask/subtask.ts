import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HasRoleDirective } from '../../shared/directives/has-role.directive';
import { SubtaskService } from '../../services/subtask';
import { AuthService } from '../../services/auth';
import { ToastService } from '../../services/toast';

@Component({
  selector: 'app-subtask',
  standalone: true,
  imports: [CommonModule, FormsModule, HasRoleDirective],
  templateUrl: './subtask.html',
  styleUrls: ['./subtask.css']
})
export class SubtaskComponent implements OnChanges {
  @Input() taskId!: number;
  @Output() subtaskChanged = new EventEmitter<void>();

  subtasks: any[] = [];
  loading = false;
  newSubtaskTitle = '';
  currentUser: any;

  constructor(
    private subtaskService: SubtaskService,
    private auth: AuthService,
    private toast: ToastService
  ) {
    this.currentUser = this.auth.getCurrentUser();
  }

  ngOnChanges() {
    if (this.taskId) this.loadSubtasks();
  }

  loadSubtasks() {
    this.loading = true;
    this.subtaskService.getSubtasks(this.taskId).subscribe({
      next: items => { this.subtasks = items; this.loading = false; },
      error: () => { this.toast.show('Failed to load subtasks', 'error'); this.loading = false; }
    });
  }

  get completedCount(): number { return this.subtasks.filter(s => s.isComplete || s.completed).length; }
  get totalCount(): number { return this.subtasks.length; }
  get progressPercent(): number { return this.totalCount > 0 ? Math.round((this.completedCount / this.totalCount) * 100) : 0; }
  get isViewer(): boolean { return this.currentUser?.role === 'VIEWER'; }

  toggle(subtask: any) {
    if (this.isViewer) return;
    this.subtaskService.toggleSubtask(subtask.id).subscribe({
      next: updated => {
        subtask.isComplete = updated.isComplete ?? updated.completed;
        subtask.completed = subtask.isComplete;
        this.subtaskChanged.emit();
      },
      error: () => this.toast.show('Failed to toggle subtask', 'error')
    });
  }

  addSubtask() {
    if (!this.newSubtaskTitle.trim()) return;
    this.subtaskService.createSubtask(this.taskId, { title: this.newSubtaskTitle }).subscribe({
      next: () => {
        this.newSubtaskTitle = '';
        this.loadSubtasks();
        this.subtaskChanged.emit();
      },
      error: () => this.toast.show('Failed to create subtask', 'error')
    });
  }

  canDelete(subtask: any): boolean {
    if (!this.currentUser) return false;
    return this.currentUser.role === 'ADMIN' ||
           this.currentUser.role === 'MANAGER' ||
           subtask.creatorId === this.currentUser.id;
  }

  deleteSubtask(subtask: any) {
    this.subtaskService.deleteSubtask(subtask.id).subscribe({
      next: () => {
        this.subtasks = this.subtasks.filter(s => s.id !== subtask.id);
        this.toast.show('Subtask removed', 'success');
        this.subtaskChanged.emit();
      },
      error: () => this.toast.show('Delete failed', 'error')
    });
  }

  getInitials(name: string): string {
    return name?.split(' ').map((n: string) => n[0]).slice(0, 2).join('').toUpperCase() || '?';
  }

  getAvatarColor(name: string): string {
    const colors = ['#6366f1','#10b981','#f59e0b','#3b82f6','#8b5cf6'];
    let h = 0;
    for (const c of name || '') h = c.charCodeAt(0) + ((h << 5) - h);
    return colors[Math.abs(h) % colors.length];
  }
}
