// import { Component, signal } from '@angular/core';
// import { RouterOutlet } from '@angular/router';
// import { NavbarComponent } from "./shared/navbar/navbar";

// @Component({
//   selector: 'app-root',
//   imports: [RouterOutlet, NavbarComponent],
//   templateUrl: './app.html',
//   styleUrl: './app.css'
// })
// export class App {
//   protected readonly title = signal('taskflow-frontend');
// }


// app.ts
import { Component, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from './shared/navbar/navbar';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, NavbarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('taskflow-frontend');

  constructor(private router: Router) {}

  get showNavbar(): boolean {
    const hiddenRoutes = ['/login', '/register'];
    return !hiddenRoutes.includes(this.router.url);
  }
}
