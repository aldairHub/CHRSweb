import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <footer class="footer">
      <p>© 2026 Universidad Técnica Estatal de Quevedo - Todos los derechos reservados</p>
    </footer>
  `,
  styles: [`
    .footer {
      text-align: center;
      padding: 20px;
      background: #f8f9fa;
      color: #666;
      margin-top: 40px;
      font-size: 14px;
    }
  `]
})
export class FooterComponent {}
