import { HttpClient } from "@angular/common/http"
import { Injectable } from "@angular/core"

@Injectable({
providedIn:'root'
})
export class CommentService{

API="http://localhost:8081/api"

constructor(private http:HttpClient){}

getComments(taskId:number){
return this.http.get<any[]>(`${this.API}/tasks/${taskId}/comments`)
}

postComment(taskId:number,body:any){
return this.http.post(`${this.API}/tasks/${taskId}/comments`,body)
}

deleteComment(id:number){
return this.http.delete(`${this.API}/comments/${id}`)
}

}
