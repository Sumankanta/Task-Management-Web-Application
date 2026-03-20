import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class TaskService {

  API = "http://localhost:8081/api/tasks";
  AUTH_API = "http://localhost:8081/api/auth";

  constructor(private http: HttpClient) {}

  private getHeaders() {
    const token = localStorage.getItem("token");

    return {
      headers: new HttpHeaders({
        Authorization: `Bearer ${token}`
      })
    };
  }

  getTasks() {
    return this.http.get<any[]>(this.API, this.getHeaders());
  }

  createTask(task: any) {
    return this.http.post(this.API, task, this.getHeaders());
  }

  deleteTask(id: number) {
    return this.http.delete(`${this.API}/${id}`, this.getHeaders());
  }

  updateTask(id:number,task:any){
    return this.http.put(`${this.API}/${id}`,task,this.getHeaders())
  }

  getUsers() {
    return this.http.get<any[]>(`${this.AUTH_API}/users`, this.getHeaders());
  }

}
