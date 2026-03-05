// src/app/dashboard/dashboard.ts
import { Component, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';
import { TaskService }     from '../services/task';
import { AuthService }     from '../services/auth';
import { CommentService }  from '../services/comment';
import { AnalyticsService, TaskSummary } from '../services/analytics';
import { ActivityService, ActivityEntry } from '../services/activity';
import { TaskDueDatePipe } from '../pipes/task-due-date-pipe';
import { RelativeTimePipe } from '../pipes/relative-time-pipe';


// Chart.js is loaded via CDN in index.html — declare global
declare const Chart: any;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent,
    TaskDueDatePipe,
    RelativeTimePipe
  ],
  templateUrl: './dashboard.html',
  styleUrls:   ['./dashboard.css']
})
export class DashboardComponent implements OnInit, AfterViewInit {

  // ── Core data ──
  tasks:    any[] = [];
  users:    any[] = [];
  comments: any[] = [];

  // ── State ──
  activeTask:    number | null = null;
  editingTaskId: number | null = null;
  filter        = 'ALL';
  currentFilter = 'ALL';

  // ── Modals ──
  showCreateModal = false;
  showEditModal   = false;

  // ── F-EXT-03: Priority sort ──
  sortByPriority = false;

  // ── F-EXT-04: Analytics ──
  showAnalytics   = false;
  analyticsLoaded = false;
  summary: TaskSummary | null = null;
  private statusChart:   any = null;
  private priorityChart: any = null;

  // ── F-EXT-05: Activity Feed ──
  activityFeed:        ActivityEntry[] = [];
  activityLoading      = false;
  showActivityPanel    = true;

  // ── F-EXT-06: Due date banner ──
  bannerDismissed = false;

  // ── Forms ──
  newTask: any = {
    title: '', description: '', dueDate: '',
    status: 'TODO', assignedTo: null, priority: 'MEDIUM'   // ← F-EXT-03
  };

  editTask: any = {
    title: '', description: '', dueDate: '',
    status: '', assignedTo: null, priority: 'MEDIUM'        // ← F-EXT-03
  };

  newComment = '';

  constructor(
    private taskService:     TaskService,
    private auth:            AuthService,
    private commentService:  CommentService,
    private analyticsService: AnalyticsService,             // ← F-EXT-04
    private activityService:  ActivityService               // ← F-EXT-05
  ) {}

  ngOnInit() {
    this.loadTasks();
    this.loadUsers();
    this.loadActivityFeed();   // ← F-EXT-05: load on init
  }

  ngAfterViewInit() {}

  // ════════════════════════════════
  // TASKS
  // ════════════════════════════════

  loadTasks() {
    this.taskService.getTasks().subscribe({
      next: (res: any) => { this.tasks = res; },
      error: (err) => console.error('Error loading tasks', err)
    });
  }

  loadUsers() {
    this.auth.getUsers().subscribe({
      next: (res: any) => this.users = res
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
      // Reload analytics if open
      if (this.showAnalytics) this.reloadAnalytics();
    });
  }

  deleteTask(id: number) {
    this.taskService.deleteTask(id).subscribe(() => {
      this.loadTasks();
      if (this.showAnalytics) this.reloadAnalytics();
    });
  }

  startEdit(task: any) {
    this.editingTaskId = task.id;
    this.editTask = {
      title:       task.title,
      description: task.description,
      dueDate:     task.dueDate,
      status:      task.status,
      assignedTo:  task.assignee?.id || null,
      priority:    task.priority || 'MEDIUM'   // ← F-EXT-03
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

  // ════════════════════════════════
  // FILTER + SORT  (F-EXT-03)
  // ════════════════════════════════

  setFilter(status: string) {
    this.filter = status;
    this.currentFilter = status;
  }

  filteredTasks() {
    let list = this.filter === 'ALL'
      ? [...this.tasks]
      : this.tasks.filter(t => t.status === this.filter);

    // F-EXT-03: client-side priority sort
    if (this.sortByPriority) {
      const order: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };
      list.sort((a, b) =>
        (order[a.priority ?? 'MEDIUM'] ?? 1) - (order[b.priority ?? 'MEDIUM'] ?? 1)
      );
    }
    return list;
  }

  togglePrioritySort() {
    this.sortByPriority = !this.sortByPriority;
  }

  // ════════════════════════════════
  // F-EXT-06: Due Date helpers
  // ════════════════════════════════

  getDueDateState(dueDate: string, status: string): 'overdue' | 'today' | 'done' | 'upcoming' {
    if (status === 'DONE') return 'done';
    const today = new Date(); today.setHours(0,0,0,0);
    const due   = new Date(dueDate); due.setHours(0,0,0,0);
    const diff  = due.getTime() - today.getTime();
    if (diff < 0)  return 'overdue';
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
  // F-EXT-04: Analytics
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
        // Wait for DOM then draw charts
        setTimeout(() => this.drawCharts(), 100);
      },
      error: (err) => console.error('Analytics error', err)
    });
  }

  private reloadAnalytics() {
    this.analyticsLoaded = false;
    this.loadAnalytics();
  }

  private drawCharts() {
    if (!this.summary) return;

    // Destroy existing charts to avoid canvas re-use error
    if (this.statusChart)   { this.statusChart.destroy();   this.statusChart = null; }
    if (this.priorityChart) { this.priorityChart.destroy(); this.priorityChart = null; }

    // Status doughnut
    const statusCanvas = document.getElementById('statusChart') as HTMLCanvasElement;
    if (statusCanvas) {
      this.statusChart = new Chart(statusCanvas, {
        type: 'doughnut',
        data: {
          labels: ['To-Do', 'In Progress', 'Done'],
          datasets: [{
            data: [
              this.summary!.byStatus.todo,
              this.summary!.byStatus.inProgress,
              this.summary!.byStatus.done
            ],
            backgroundColor: ['#6366f1', '#f59e0b', '#10b981'],
            borderWidth: 0,
            hoverOffset: 6
          }]
        },
        options: {
          cutout: '68%',
          plugins: {
            legend: { position: 'bottom', labels: { padding: 16, font: { size: 12 } } }
          }
        }
      });
    }

    // Priority bar
    const priorityCanvas = document.getElementById('priorityChart') as HTMLCanvasElement;
    if (priorityCanvas) {
      this.priorityChart = new Chart(priorityCanvas, {
        type: 'bar',
        data: {
          labels: ['High', 'Medium', 'Low'],
          datasets: [{
            label: 'Tasks',
            data: [
              this.summary!.byPriority.high,
              this.summary!.byPriority.medium,
              this.summary!.byPriority.low
            ],
            backgroundColor: ['#ef4444', '#f59e0b', '#10b981'],
            borderRadius: 6,
            borderSkipped: false
          }]
        },
        options: {
          plugins: { legend: { display: false } },
          scales: {
            y: {
              beginAtZero: true,
              ticks: { stepSize: 1 },
              grid: { color: 'rgba(0,0,0,0.05)' }
            },
            x: { grid: { display: false } }
          }
        }
      });
    }
  }

  // ════════════════════════════════
  // F-EXT-05: Activity Feed
  // ════════════════════════════════

  loadActivityFeed() {
    this.activityLoading = true;
    this.activityService.getFeed().subscribe({
      next:  (res) => { this.activityFeed = res; this.activityLoading = false; },
      error: (err) => { console.error('Activity feed error', err); this.activityLoading = false; }
    });
  }

  // Map action code → icon colour class
  actionColor(code: string): string {
    const map: Record<string, string> = {
      TASK_CREATED:          'act-green',
      TASK_STATUS_CHANGED:   'act-amber',
      TASK_ASSIGNED:         'act-purple',
      TASK_PRIORITY_CHANGED: 'act-red',
      COMMENT_ADDED:         'act-blue',
      TASK_DELETED:          'act-grey'
    };
    return map[code] ?? 'act-grey';
  }

  // Map action code → label
  actionLabel(code: string): string {
    const map: Record<string, string> = {
      TASK_CREATED:          'Created',
      TASK_STATUS_CHANGED:   'Status',
      TASK_ASSIGNED:         'Assigned',
      TASK_PRIORITY_CHANGED: 'Priority',
      COMMENT_ADDED:         'Comment',
      TASK_DELETED:          'Deleted'
    };
    return map[code] ?? code;
  }

  // ════════════════════════════════
  // COMMENTS
  // ════════════════════════════════

  toggleComments(taskId: number) {
    if (this.activeTask === taskId) { this.activeTask = null; return; }
    this.activeTask = taskId;
    this.commentService.getComments(taskId).subscribe(res => { this.comments = res; });
  }

  postComment(taskId: number) {
    if (!this.newComment.trim()) return;
    this.commentService.postComment(taskId, { body: this.newComment }).subscribe(() => {
      this.newComment = '';
      this.commentService.getComments(taskId).subscribe(res => { this.comments = res; });
      this.loadActivityFeed();  // refresh feed after comment
    });
  }
}
