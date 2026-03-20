import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HasRoleDirective } from '../../shared/directives/has-role.directive';
import { AttachmentService } from '../../services/attachment';
import { AuthService } from '../../services/auth';
import { ToastService } from '../../services/toast';

const ALLOWED_TYPES = [
  'image/jpeg','image/png','image/gif',
  'application/pdf',
  'application/msword','application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'text/plain','application/zip'
];
const MAX_SIZE = 5 * 1024 * 1024;
const MAX_FILES = 5;

@Component({
  selector: 'app-attachment',
  standalone: true,
  imports: [CommonModule, FormsModule, HasRoleDirective],
  templateUrl: './attachment.html',
  styleUrls: ['./attachment.css']
})
export class AttachmentComponent implements OnChanges {
  @Input() taskId!: number;

  attachments: any[] = [];
  loading = false;
  uploading = false;
  dragOver = false;
  uploadError = '';
  currentUser: any;

  constructor(
    private attachmentService: AttachmentService,
    private auth: AuthService,
    private toast: ToastService
  ) {
    this.currentUser = this.auth.getCurrentUser();
  }

  ngOnChanges() {
    if (this.taskId) this.loadAttachments();
  }

  loadAttachments() {
    this.loading = true;
    this.attachmentService.getAttachments(this.taskId).subscribe({
      next: items => { this.attachments = items; this.loading = false; },
      error: () => { this.toast.show('Failed to load attachments', 'error'); this.loading = false; }
    });
  }

  get isMaxReached(): boolean { return this.attachments.length >= MAX_FILES; }

  get canUpload(): boolean {
    return !['VIEWER'].includes(this.currentUser?.role || '');
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.dragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.handleFile(file);
  }

  onFileChange(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.handleFile(file);
  }

  private handleFile(file: File) {
    this.uploadError = '';
    if (this.isMaxReached) { this.uploadError = 'Maximum 5 files reached.'; return; }
    if (file.size > MAX_SIZE) { this.uploadError = `File too large. Max allowed is 5 MB.`; return; }
    if (!ALLOWED_TYPES.includes(file.type)) { this.uploadError = 'File type not allowed.'; return; }
    this.upload(file);
  }

  private upload(file: File) {
    this.uploading = true;
    this.attachmentService.uploadAttachment(this.taskId, file).subscribe({
      next: () => {
        this.uploading = false;
        this.toast.show('File uploaded', 'success');
        this.loadAttachments();
      },
      error: () => { this.uploading = false; this.toast.show('Upload failed', 'error'); }
    });
  }

  download(att: any) {
    this.attachmentService.downloadAttachment(att.id).subscribe({
      next: response => {
        const blob = response.body!;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = att.fileName || att.originalFileName || 'download';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.toast.show('Download failed', 'error')
    });
  }

  canDelete(att: any): boolean {
    if (!this.currentUser) return false;
    return this.currentUser.role === 'ADMIN' ||
           this.currentUser.role === 'MANAGER' ||
           att.uploaderId === this.currentUser.id;
  }

  deleteAttachment(att: any) {
    this.attachmentService.deleteAttachment(att.id).subscribe({
      next: () => { this.toast.show('Attachment deleted', 'success'); this.loadAttachments(); },
      error: () => this.toast.show('Delete failed', 'error')
    });
  }

  fileTypeIcon(mime: string): string {
    if (mime?.startsWith('image/')) return '🖼️';
    if (mime === 'application/pdf') return '📄';
    if (mime?.includes('word') || mime?.includes('document')) return '📝';
    if (mime?.includes('excel') || mime?.includes('sheet')) return '📊';
    if (mime === 'text/plain') return '📃';
    if (mime === 'application/zip') return '🗜️';
    return '📎';
  }

  fileTypeColor(mime: string): string {
    if (mime?.startsWith('image/')) return '#3b82f6';
    if (mime === 'application/pdf') return '#ef4444';
    if (mime?.includes('word') || mime?.includes('document')) return '#2563eb';
    if (mime?.includes('excel') || mime?.includes('sheet')) return '#10b981';
    return '#64748b';
  }

  formatSize(bytes: number): string {
    if (!bytes) return '—';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
