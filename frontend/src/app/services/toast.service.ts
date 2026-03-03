import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info' | 'loading';

export interface Toast {
  id: number;
  type: ToastType;
  title: string;
  message?: string;
  duration?: number; // 0 = no se cierra solo
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private counter = 0;
  toasts = signal<Toast[]>([]);

  private add(toast: Omit<Toast, 'id'>): number {
    const id = ++this.counter;
    this.toasts.update(list => [...list, { ...toast, id }]);
    if (toast.duration !== 0) {
      setTimeout(() => this.remove(id), toast.duration ?? 4000);
    }
    return id;
  }

  success(title: string, message?: string) { return this.add({ type: 'success', title, message }); }
  error(title: string, message?: string)   { return this.add({ type: 'error',   title, message, duration: 6000 }); }
  warning(title: string, message?: string) { return this.add({ type: 'warning', title, message, duration: 5000 }); }
  info(title: string, message?: string)    { return this.add({ type: 'info',    title, message }); }

  /** Retorna el ID para cerrarlo manualmente cuando termine */
  loading(title: string, message?: string): number {
    return this.add({ type: 'loading', title, message, duration: 0 });
  }

  remove(id: number) {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }
}
