import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from './shared/navbar/navbar';
import { ToastComponent } from './shared/toast/toast';
import { ThemeService } from './services/theme';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, NavbarComponent, ToastComponent],
  template: `
    @if (showNavbar) {
      <app-navbar />
    }
    <router-outlet />
    <app-toast />
  `,
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('taskflow-frontend');

  constructor(private router: Router, private themeService: ThemeService) {}

  ngOnInit() {
    this.themeService.init();
  }

  get showNavbar(): boolean {
    const hiddenRoutes = ['/login', '/register'];
    return !hiddenRoutes.includes(this.router.url);
  }
}
