import { Injectable, signal } from '@angular/core';
import { AuthService } from './auth';

export type ThemeMode = 'LIGHT' | 'DARK' | 'SYSTEM';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'taskflow_theme';
  currentTheme = signal<ThemeMode>('SYSTEM');

  constructor(private auth: AuthService) {}

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
