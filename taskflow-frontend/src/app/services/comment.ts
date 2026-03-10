import { HttpClient, HttpHeaders } from "@angular/common/http"
import { Injectable } from "@angular/core"

@Injectable({
  providedIn: 'root'
})
export class CommentService {

  API = "http://localhost:8081/api"

  constructor(private http: HttpClient) { }

  // ── Auth headers — was missing, causing silent 401 on first click ──
  private getHeaders() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        Authorization: `Bearer ${token}`
      })
    };
  }

  getComments(taskId: number) {
    return this.http.get<any[]>(
      `${this.API}/tasks/${taskId}/comments`,
      this.getHeaders()
    );
  }

  postComment(taskId: number, body: any) {
    return this.http.post(
      `${this.API}/tasks/${taskId}/comments`,
      body,
      this.getHeaders()
    );
  }

  deleteComment(id: number) {
    return this.http.delete(
      `${this.API}/comments/${id}`,
      this.getHeaders()
    );
  }

}
