import { Injectable, signal } from '@angular/core';
import { AuthService } from './auth';

export type ThemeMode = 'LIGHT' | 'DARK' | 'SYSTEM';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'taskflow_theme';
  currentTheme = signal<ThemeMode>('SYSTEM');

  constructor(private auth: AuthService) { }

  init() {
    const saved = (localStorage.getItem(this.STORAGE_KEY) as ThemeMode) || 'SYSTEM';
    this.setTheme(saved, false);
  }

  setTheme(theme: ThemeMode, persist = true) {
    this.currentTheme.set(theme);
    if (persist) {
      localStorage.setItem(this.STORAGE_KEY, theme);
    }
    this.applyTheme(theme);
  }

  private applyTheme(theme: ThemeMode) {
    const html = document.documentElement;
    const body = document.body;

    const applyDark = (isDark: boolean) => {
      html.setAttribute('data-theme', isDark ? 'dark' : 'light');
      if (isDark) body.classList.add('dark');
      else body.classList.remove('dark');
    };

    if (theme === 'DARK') {
      applyDark(true);
    } else if (theme === 'LIGHT') {
      applyDark(false);
    } else {
      // SYSTEM
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      applyDark(prefersDark);

      // Listen for OS preference changes
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
        if (this.currentTheme() === 'SYSTEM') {
          applyDark(e.matches);
        }
      }, { once: true });
    }
  }
}


// import { Injectable, signal } from '@angular/core';

// export type ThemeMode = 'LIGHT' | 'DARK' | 'SYSTEM';

// @Injectable({ providedIn: 'root' })
// export class ThemeService {
//   private _theme = signal<ThemeMode>('DARK');

//   currentTheme = this._theme.asReadonly();

//   constructor() {
//     // Load saved preference on startup
//     const saved = localStorage.getItem('taskflow-theme') as ThemeMode | null;
//     if (saved && ['LIGHT', 'DARK', 'SYSTEM'].includes(saved)) {
//       this.applyTheme(saved);
//     } else {
//       this.applyTheme('DARK');
//     }
//   }

//   setTheme(theme: ThemeMode) {
//     localStorage.setItem('taskflow-theme', theme);
//     this.applyTheme(theme);
//   }

//   private applyTheme(theme: ThemeMode) {
//     this._theme.set(theme);

//     const root = document.documentElement;
//     root.classList.remove('theme-light', 'theme-dark');

//     if (theme === 'SYSTEM') {
//       const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
//       root.classList.add(prefersDark ? 'theme-dark' : 'theme-light');
//     } else {
//       root.classList.add(theme === 'DARK' ? 'theme-dark' : 'theme-light');
//     }
//   }
// }