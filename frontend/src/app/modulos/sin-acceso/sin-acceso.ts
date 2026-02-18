// src/app/modulos/sin-acceso/sin-acceso.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-sin-acceso',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="no-access-wrapper">
      <div class="no-access-card">
        <div class="icon-circle">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <h1>Acceso no configurado</h1>
        <p>
          Tu usuario está activo pero aún no tiene un rol de aplicación asignado
          o el rol asignado no tiene una pantalla configurada en el sistema.
        </p>
        <p class="hint">
          Contacta al administrador del sistema para que te asigne el rol correspondiente.
        </p>
        <button class="btn-logout" (click)="salir()">
          Cerrar sesión
        </button>
      </div>
    </div>
  `,
  styles: [`
    .no-access-wrapper {
      min-height: 100vh;
      background: #f5f7f5;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
    }

    .no-access-card {
      background: #ffffff;
      border: 1px solid #B9F8CF;
      border-radius: 20px;
      padding: 3rem 2.5rem;
      max-width: 480px;
      width: 100%;
      text-align: center;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
    }

    .icon-circle {
      width: 96px;
      height: 96px;
      background: #EEF9EC;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 1.75rem;
      svg { stroke: #016630; }
    }

    h1 {
      font-size: 1.5rem;
      font-weight: 700;
      color: #016630;
      margin: 0 0 1rem 0;
    }

    p {
      font-size: 0.95rem;
      color: #536b50;
      line-height: 1.6;
      margin: 0 0 0.75rem 0;
    }

    .hint {
      font-size: 0.85rem;
      color: #9ca3af;
      font-style: italic;
      margin-bottom: 2rem;
    }

    .btn-logout {
      padding: 0.75rem 2rem;
      background: #00A63E;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;

      &:hover { background: #016630; }
    }
  `]
})
export class SinAccesoComponent {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  salir(): void {
    this.authService.logoutYSalir();
  }
}
