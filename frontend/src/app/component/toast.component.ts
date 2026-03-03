import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast toast--{{ toast.type }}" role="alert">
          <div class="toast__icon">
            <!-- SUCCESS -->
            @if (toast.type === 'success') {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            }
            <!-- ERROR -->
            @if (toast.type === 'error') {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            }
            <!-- WARNING -->
            @if (toast.type === 'warning') {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
              </svg>
            }
            <!-- INFO -->
            @if (toast.type === 'info') {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            }
            <!-- LOADING -->
            @if (toast.type === 'loading') {
              <div class="toast__spinner"></div>
            }
          </div>
          <div class="toast__body">
            <span class="toast__title">{{ toast.title }}</span>
            @if (toast.message) {
              <span class="toast__msg">{{ toast.message }}</span>
            }
          </div>
          @if (toast.type !== 'loading') {
            <button class="toast__close" (click)="toastService.remove(toast.id)" aria-label="Cerrar">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 1.25rem;
      right: 1.25rem;
      z-index: 99999;
      display: flex;
      flex-direction: column;
      gap: 0.625rem;
      max-width: 380px;
      width: calc(100vw - 2.5rem);
      pointer-events: none;
    }

    .toast {
      display: flex;
      align-items: flex-start;
      gap: 0.875rem;
      padding: 0.875rem 1rem;
      border-radius: 12px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.12);
      border: 1px solid transparent;
      animation: toastIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
      pointer-events: all;
    }

    /* Colores por tipo — usando la paleta UTEQ */
    .toast--success {
      background: #f0fdf4;
      border-color: #bbf7d0;
      .toast__icon { color: #16a34a; }
    }
    .toast--error {
      background: #fff5f5;
      border-color: #fecaca;
      .toast__icon { color: #dc2626; }
    }
    .toast--warning {
      background: #fffbeb;
      border-color: #fde68a;
      .toast__icon { color: #d97706; }
    }
    .toast--info {
      background: #EEF9EC;
      border-color: #B9F8CF;
      .toast__icon { color: #016630; }
    }
    .toast--loading {
      background: #EEF9EC;
      border-color: #B9F8CF;
    }

    .toast__icon {
      flex-shrink: 0;
      width: 22px;
      height: 22px;
      margin-top: 1px;
      svg { width: 22px; height: 22px; }
    }

    .toast__spinner {
      width: 20px;
      height: 20px;
      border: 2.5px solid #B9F8CF;
      border-top-color: #00A63E;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    .toast__body {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
      min-width: 0;
    }

    .toast__title {
      font-size: 0.875rem;
      font-weight: 600;
      color: #1a1a1a;
      line-height: 1.4;
    }

    .toast__msg {
      font-size: 0.8125rem;
      color: #536b50;
      line-height: 1.4;
    }

    .toast__close {
      flex-shrink: 0;
      width: 28px;
      height: 28px;
      border: none;
      background: transparent;
      cursor: pointer;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #536b50;
      transition: all 0.15s;
      padding: 0;
      margin-top: -2px;
      svg { width: 16px; height: 16px; }
      &:hover { background: rgba(0,0,0,0.07); color: #1a1a1a; }
    }

    @keyframes toastIn {
      from { opacity: 0; transform: translateX(20px) scale(0.95); }
      to   { opacity: 1; transform: translateX(0) scale(1); }
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class ToastComponent {
  toastService = inject(ToastService);
}
