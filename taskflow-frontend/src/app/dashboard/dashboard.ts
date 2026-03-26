// src/app/dashboard/dashboard.ts  (Week 2 update)
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { switchMap } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';
import { TaskService } from '../services/task';
import { AuthService } from '../services/auth';
import { CommentService } from '../services/comment';
import { SubtaskService } from '../services/subtask';
import { RelativeTimePipe } from '../pipes/relative-time-pipe';
import { TaskDueDatePipe } from '../pipes/task-due-date-pipe';
import { ActivityEntry, ActivityService } from '../services/activity';
import { TaskSummary, AnalyticsService } from '../services/analytics';
import { AttachmentComponent } from './attachment/attachment';
import { SubtaskComponent } from './subtask/subtask';
import { TimeTrackingComponent } from './time-tracking/time-tracking';
import { TeamService } from '../services/team';

declare const Chart: any;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  encapsulation: ViewEncapsulation.None,
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent,
    TaskDueDatePipe,
    RelativeTimePipe,
    AttachmentComponent,
    SubtaskComponent,
    TimeTrackingComponent
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardComponent implements OnInit {

  // ── Core data ──
  tasks: any[] = [];
  users: any[] = [];
  comments: any[] = [];
  teams: any[] = [];

  // ── Task detail modal ──
  showDetailModal = false;
  detailTask: any = null;
  detailTab: 'comments' | 'subtasks' | 'attachments' | 'time' = 'comments';
  subtaskSummaries: Record<number, { total: number; completed: number }> = {};

  // ── State ──
  activeTask: number | null = null;
  editingTaskId: number | null = null;
  filter = 'ALL';
  currentFilter = 'ALL';
  currentTeamFilter: number | null = null;

  // ── Modals ──
  showCreateModal = false;
  showEditModal = false;

  // ── Priority sort ──
  sortByPriority = false;

  // ── Analytics ──
  showAnalytics = false;
  analyticsLoaded = false;
  summary: TaskSummary | null = null;
  private statusChart: any = null;
  private priorityChart: any = null;

  // ── Activity Feed ──
  activityFeed: ActivityEntry[] = [];
  activityLoading = false;

  // ── Due date banner ──
  bannerDismissed = false;

  // ── Forms ──
  newTask: any = {
    title: '', description: '', dueDate: '',
    status: 'TODO', assignedTo: null, teamId: null, priority: 'MEDIUM'
  };

  editTask: any = {
    title: '', description: '', dueDate: '',
    status: '', assignedTo: null, teamId: null, priority: 'MEDIUM'
  };

  newComment = '';

  constructor(
    private taskService: TaskService,
    private auth: AuthService,
    private commentService: CommentService,
    private analyticsService: AnalyticsService,
    private activityService: ActivityService,
    private subtaskService: SubtaskService,
    private teamService: TeamService
  ) { }

  ngOnInit() {
    if (localStorage.getItem('darkMode') === 'true') {
      document.body.classList.add('dark');
    }
    this.loadTasks();
    this.loadUsers();
    this.loadTeams();
    this.loadActivityFeed();
  }

  // ════════════════════════════════
  // TASKS
  // ════════════════════════════════

  loadTasks() {
    this.taskService.getTasks().subscribe({
      next: (res: any) => {
        this.tasks = res;
        this.loadSubtaskSummaries();
      },
      error: (err) => console.error('Error loading tasks', err)
    });
  }

  loadSubtaskSummaries() {
    this.tasks.forEach(task => {
      this.subtaskService.getSubtaskSummary(task.id).subscribe({
        next: summary => {
          this.subtaskSummaries = { ...this.subtaskSummaries, [task.id]: summary };
        },
        error: () => { }
      });
    });
  }

  getSubtaskSummary(taskId: number) {
    return this.subtaskSummaries[taskId];
  }

  getSubtaskProgress(taskId: number): number {
    const s = this.subtaskSummaries[taskId];
    if (!s || s.total === 0) return 0;
    return Math.round((s.completed / s.total) * 100);
  }

  loadUsers() {
    this.taskService.getUsers().subscribe({
      next: (res: any) => this.users = res,
      error: (err) => console.error('Error loading users', err)
    });
  }

  loadTeams() {
    this.teamService.getTeams().subscribe({
      next: (res: any) => this.teams = res,
      error: (err) => console.error('Error loading teams', err)
    });
  }

  createTask() {
    if (!this.newTask.title.trim()) return;
    this.taskService.createTask(this.newTask).subscribe(() => {
      this.loadTasks();
      this.showCreateModal = false;
      this.newTask = {
        title: '', description: '', dueDate: '',
        status: 'TODO', assignedTo: null, priority: 'MEDIUM'
      };
      if (this.showAnalytics) this.reloadAnalytics();
      this.loadActivityFeed();
    });
  }

  deleteTask(id: number) {
    this.taskService.deleteTask(id).subscribe(() => {
      this.tasks = this.tasks.filter(t => t.id !== id);
      if (this.showAnalytics) this.reloadAnalytics();
      this.loadActivityFeed();
      if (this.detailTask?.id === id) this.closeDetailModal();
    });
  }

  startEdit(task: any) {
    this.editingTaskId = task.id;
    this.editTask = {
      title: task.title,
      description: task.description,
      dueDate: task.dueDate,
      status: task.status,
      assignedTo: task.assignee?.id || null,
      priority: task.priority || 'MEDIUM'
    };
    this.showEditModal = true;
  }

  cancelEdit() {
    this.editingTaskId = null;
    this.showEditModal = false;
  }

  updateTask(id: number) {
    this.taskService.updateTask(id, this.editTask).subscribe(() => {
      this.editingTaskId = null;
      this.showEditModal = false;
      this.loadTasks();
      if (this.showAnalytics) this.reloadAnalytics();
      this.loadActivityFeed();
    });
  }

  // ── Task Detail Modal ──
  openDetail(task: any) {
    this.detailTask = task;
    this.detailTab = 'comments';
    this.showDetailModal = true;
    this.activeTask = task.id;
    this.commentService.getComments(task.id).subscribe(res => { this.comments = res; });
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.detailTask = null;
    this.activeTask = null;
  }

  onSubtaskChanged(taskId: number) {
    this.subtaskService.getSubtaskSummary(taskId).subscribe(summary => {
      this.subtaskSummaries = { ...this.subtaskSummaries, [taskId]: summary };
    });
  }

  closeCreateModal(e: MouseEvent) {
    if ((e.target as HTMLElement).classList.contains('modal-overlay'))
      this.showCreateModal = false;
  }

  closeEditModal(e: MouseEvent) {
    if ((e.target as HTMLElement).classList.contains('modal-overlay'))
      this.cancelEdit();
  }

  closeDetailOverlay(e: MouseEvent) {
    if ((e.target as HTMLElement).classList.contains('modal-overlay'))
      this.closeDetailModal();
  }

  // ════════════════════════════════
  // FILTER + SORT
  // ════════════════════════════════

  setFilter(status: string) {
    this.filter = status;
    this.currentFilter = status;
  }

  onTeamFilterChange() { }

  filteredTasks() {
    let list = this.currentFilter === 'ALL'
      ? [...this.tasks]
      : this.tasks.filter(t => t.status === this.currentFilter);

    if (this.currentTeamFilter !== null) {
      list = list.filter(t => t.teamId == this.currentTeamFilter);
    }

    if (this.sortByPriority) {
      const order: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };
      list.sort((a,b) => (order[a.priority ?? 'MEDIUM'] ?? 1) - (order[b.priority ?? 'MEDIUM'] ?? 1));
    }
    return list;
  }

  togglePrioritySort() { this.sortByPriority = !this.sortByPriority; }

  // ════════════════════════════════
  // Due Date helpers
  // ════════════════════════════════

  getDueDateState(dueDate: string, status: string): 'overdue' | 'today' | 'done' | 'upcoming' {
    if (status === 'DONE') return 'done';
    const today = new Date(); today.setHours(0, 0, 0, 0);
    const due = new Date(dueDate); due.setHours(0, 0, 0, 0);
    const diff = due.getTime() - today.getTime();
    if (diff < 0) return 'overdue';
    if (diff === 0) return 'today';
    return 'upcoming';
  }

  get overdueCount(): number {
    return this.tasks.filter(t =>
      this.getDueDateState(t.dueDate, t.status) === 'overdue'
    ).length;
  }

  get dueTodayCount(): number {
    return this.tasks.filter(t =>
      this.getDueDateState(t.dueDate, t.status) === 'today'
    ).length;
  }

  get showBanner(): boolean {
    return !this.bannerDismissed && (this.overdueCount > 0 || this.dueTodayCount > 0);
  }

  dismissBanner() { this.bannerDismissed = true; }

  // ════════════════════════════════
  // Analytics
  // ════════════════════════════════

  toggleAnalytics() {
    this.showAnalytics = !this.showAnalytics;
    if (this.showAnalytics && !this.analyticsLoaded) {
      this.loadAnalytics();
    }
  }

  private loadAnalytics() {
    this.analyticsService.getSummary().subscribe({
      next: (data) => {
        this.summary = data;
        this.analyticsLoaded = true;
        setTimeout(() => this.drawCharts(), 100);
      },
      error: (err) => console.error('Analytics error', err)
    });
  }

  private reloadAnalytics() {
    this.analyticsLoaded = false;
    this.summary = null;
    this.loadAnalytics();
  }

  private drawCharts() {
    if (!this.summary) return;
    if (this.statusChart) { this.statusChart.destroy(); this.statusChart = null; }
    if (this.priorityChart) { this.priorityChart.destroy(); this.priorityChart = null; }

    const statusCanvas = document.getElementById('statusChart') as HTMLCanvasElement;
    if (statusCanvas) {
      this.statusChart = new Chart(statusCanvas, {
        type: 'doughnut',
        data: {
          labels: ['To-Do', 'In Progress', 'Done'],
          datasets: [{
            data: [this.summary!.todo, this.summary!.inProgress, this.summary!.done],
            backgroundColor: ['#6366f1', '#f59e0b', '#10b981'],
            borderWidth: 0, hoverOffset: 6
          }]
        },
        options: {
          maintainAspectRatio: false, cutout: '70%',
          plugins: { legend: { position: 'bottom', labels: { padding: 14, font: { size: 11 }, boxWidth: 10 } } }
        }
      });
    }

    const priorityCanvas = document.getElementById('priorityChart') as HTMLCanvasElement;
    if (priorityCanvas) {
      this.priorityChart = new Chart(priorityCanvas, {
        type: 'bar',
        data: {
          labels: ['High', 'Medium', 'Low'],
          datasets: [{
            label: 'Tasks',
            data: [this.summary!.high, this.summary!.medium, this.summary!.low],
            backgroundColor: ['#ef4444', '#f59e0b', '#10b981'],
            borderRadius: 8, borderSkipped: false
          }]
        },
        options: {
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: {
            y: { beginAtZero: true, ticks: { stepSize: 1, font: { size: 11 } }, grid: { color: 'rgba(0,0,0,0.04)' } },
            x: { grid: { display: false }, ticks: { font: { size: 11 } } }
          }
        }
      });
    }
  }

  // ════════════════════════════════
  // Activity Feed
  // ════════════════════════════════

  loadActivityFeed() {
    this.activityLoading = true;
    this.activityService.getFeed().subscribe({
      next: (res) => { this.activityFeed = res; this.activityLoading = false; },
      error: (err) => { console.error('Activity feed error', err); this.activityLoading = false; }
    });
  }

  actionColor(code: string): string {
    const map: Record<string, string> = {
      TASK_CREATED: 'act-green', TASK_STATUS_CHANGED: 'act-amber',
      TASK_ASSIGNED: 'act-purple', TASK_PRIORITY_CHANGED: 'act-red',
      COMMENT_ADDED: 'act-blue', TASK_DELETED: 'act-grey'
    };
    return map[code] ?? 'act-grey';
  }

  // ════════════════════════════════
  // Comments
  // ════════════════════════════════

  postComment(taskId: number) {
    if (!this.newComment.trim()) return;
    const body = this.newComment;
    this.newComment = '';

    this.commentService.postComment(taskId, { body }).pipe(
      switchMap(() => this.commentService.getComments(taskId))
    ).subscribe({
      next: (res) => { this.comments = res; this.loadActivityFeed(); },
      error: (err) => { console.error('Post comment error', err); this.newComment = body; }
    });
  }
}
