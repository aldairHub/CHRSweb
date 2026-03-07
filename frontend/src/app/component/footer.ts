import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  encapsulation: ViewEncapsulation.None,
  template: `
    <footer class="app-footer">
      <p>© 2026 Universidad Técnica Estatal de Quevedo - Todos los derechos reservados</p>
    </footer>
  `,
  styles: [`
    app-footer { display: block; }

    .app-footer {
      text-align: center;
      padding: 20px;
      background: #f8f9fa;
      border-top: 1px solid #e2e8f0;
      color: #666;
      font-size: 14px;
      transition: background 0.3s, color 0.3s;
    }

    html.dark .app-footer {
      background: #111111 !important;
      border-top: 1px solid #2a2a2a !important;
      color: #444444 !important;
    }
  `]
})
export class FooterComponent {}
