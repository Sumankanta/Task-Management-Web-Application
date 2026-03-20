import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

const BASE = 'http://localhost:8081/api';

@Injectable({ providedIn: 'root' })
export class AttachmentService {
  constructor(private http: HttpClient) {}

  getAttachments(taskId: number) {
    return this.http.get<any[]>(`${BASE}/tasks/${taskId}/attachments`);
  }

  uploadAttachment(taskId: number, file: File) {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<any>(`${BASE}/tasks/${taskId}/attachments`, form);
  }

  downloadAttachment(attachmentId: number) {
    return this.http.get(`${BASE}/attachments/${attachmentId}/download`, {
      responseType: 'blob',
      observe: 'response'
    });
  }

  deleteAttachment(attachmentId: number) {
    return this.http.delete(`${BASE}/attachments/${attachmentId}`);
  }
}
