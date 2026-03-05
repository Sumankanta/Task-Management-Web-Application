import { Injectable } from '@angular/core'
import { HttpClient } from '@angular/common/http'

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  API = 'http://localhost:8081/api/auth'

  constructor(private http: HttpClient) {}

  register(data:any){
    return this.http.post(`${this.API}/register`,data)
  }

  login(data:any){
    return this.http.post(`${this.API}/login`,data)
  }

  saveToken(token:string){
    localStorage.setItem('token',token)
  }

  getToken(){
    return localStorage.getItem('token')
  }

  logout(){
    localStorage.removeItem('token')
  }

  isLoggedIn(){
    return !!this.getToken()
  }

  getUsers(){
  return this.http.get<any[]>("http://localhost:8081/api/auth/users")
}

}
