import { Component } from '@angular/core';
import { AuthService } from '../../services/auth';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector:'app-login',
  standalone:true,
  imports:[FormsModule,CommonModule,RouterModule],
  templateUrl:'./login.html',
  styleUrls:['./login.css']
})
export class LoginComponent{

data:any={
email:'',
password:''
}

constructor(
private auth:AuthService,
private router:Router
){}

login(){

console.log("Login clicked",this.data)

this.auth.login(this.data).subscribe({

next:(res:any)=>{

localStorage.setItem('token',res.token)

this.router.navigate(['/dashboard'])

},

error:()=>{
alert("Invalid credentials")
}

})

}

}
