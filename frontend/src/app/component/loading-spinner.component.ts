import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Componente reutilizable de loading para tablas de datos.
 *
 * Uso:
 *   <app-loading-spinner *ngIf="isLoading" />
 *   <app-loading-spinner [mensaje]="'Cargando usuarios...'" *ngIf="isLoading" />
 */
@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="loading-wrapper" [class.loading-wrapper--inline]="inline">
      <div class="loading-spinner">
        <div class="spinner-ring"></div>
        <p class="spinner-text">{{ mensaje }}</p>
      </div>
    </div>
  `,
  styles: [`
    .loading-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 3rem 1rem;
      width: 100%;
    }

    .loading-wrapper--inline {
      padding: 1rem;
    }

    .loading-spinner {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.875rem;
    }

    .spinner-ring {
      width: 44px;
      height: 44px;
      border: 3.5px solid #d1fae5;
      border-top-color: #00A63E;
      border-radius: 50%;
      animation: spin 0.75s linear infinite;
    }

    .spinner-text {
      font-size: 0.875rem;
      color: #6b7280;
      margin: 0;
      font-weight: 500;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class LoadingSpinnerComponent {
  @Input() mensaje = 'Cargando...';
  @Input() inline  = false;
}
