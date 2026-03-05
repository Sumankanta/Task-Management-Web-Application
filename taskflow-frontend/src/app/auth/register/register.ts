import { Component } from '@angular/core';
import { AuthService } from '../../services/auth';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterModule],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {

  data: any = {
    fullName: '',
    email: '',
    password: ''
  }

  constructor(
    private auth: AuthService,
    private router: Router
  ) { }

  register() {

    console.log("Register clicked", this.data)

    this.auth.register(this.data).subscribe({

      next: () => {

        alert("Registration successful")

        this.router.navigate(['/login'])

      },

      error: () => {
        alert("Registration failed")
      }

    })

  }

}
