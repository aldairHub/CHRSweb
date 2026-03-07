import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'uteq-theme';

  isDark = signal<boolean>(false);

  constructor() {
    // Leer preferencia guardada (o del sistema si no hay)
    const saved = localStorage.getItem(this.STORAGE_KEY);
    if (saved !== null) {
      this.isDark.set(saved === 'dark');
    } else {
      // Usar preferencia del sistema operativo por defecto
      this.isDark.set(window.matchMedia('(prefers-color-scheme: dark)').matches);
    }
    this.applyTheme();
  }

  toggle(): void {
    this.isDark.set(!this.isDark());
    localStorage.setItem(this.STORAGE_KEY, this.isDark() ? 'dark' : 'light');
    this.applyTheme();
  }

  private applyTheme(): void {
    if (this.isDark()) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }
}
