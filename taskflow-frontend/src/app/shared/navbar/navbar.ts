// import { Component } from '@angular/core';
// import { Router } from '@angular/router';

// @Component({
//   selector:'app-navbar',
//   standalone:true,
//   templateUrl:'./navbar.html',
//   styleUrls:['./navbar.css']
// })
// export class NavbarComponent{

// constructor(private router:Router){}

// logout(){

// localStorage.removeItem('token');

// this.router.navigate(['/login']);

// }

// }


import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css']
})
export class NavbarComponent {

  menuOpen  = false;
  darkMode  = false;

  constructor(private router: Router) {
    // Restore dark mode preference on load
    const saved = localStorage.getItem('darkMode');
    if (saved === 'true') {
      this.darkMode = true;
      document.body.classList.add('dark');
    }
  }

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  toggleDarkMode(): void {
    this.darkMode = !this.darkMode;
    if (this.darkMode) {
      document.body.classList.add('dark');
      localStorage.setItem('darkMode', 'true');
    } else {
      document.body.classList.remove('dark');
      localStorage.setItem('darkMode', 'false');
    }
  }

  logout(): void {
    this.menuOpen = false;
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

}
