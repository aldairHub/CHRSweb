// revisor.ts  — agrega 'solicitudes-docente' al SVG_MAP
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

interface DashCard {
  titulo:      string;
  descripcion: string;
  ruta:        string;
  svgPaths:    Array<{ d: string }>;
}

const SVG_MAP: Record<string, Array<{ d: string }>> = {

  'prepostulaciones': [
    { d: 'M26.6667 28V25.3333C26.6667 23.9188 26.1048 22.5623 25.1046 21.5621C24.1044 20.5619 22.7479 20 21.3334 20H10.6667C9.25222 20 7.89567 20.5619 6.89547 21.5621C5.89528 22.5623 5.33337 23.9188 5.33337 25.3333V28' },
    { d: 'M16 14.6667C18.9455 14.6667 21.3333 12.2789 21.3333 9.33333C21.3333 6.38781 18.9455 4 16 4C13.0545 4 10.6667 6.38781 10.6667 9.33333C10.6667 12.2789 13.0545 14.6667 16 14.6667Z' },
    { d: 'M21.3333 29.3333L24 32L29.3333 26.6667' },
  ],

  'convocatorias': [
    { d: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2' },
    { d: 'M12 11v6m-3-3h6' },
  ],

  'solicitudes-docente': [
    { d: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' },
    { d: 'M9 12l2 2 4-4' },
  ],

};

const SVG_FALLBACK: Array<{ d: string }> = [
  { d: 'M16 2.6667C8.6364 2.6667 2.6667 8.6364 2.6667 16C2.6667 23.3636 8.6364 29.3333 16 29.3333C23.3636 29.3333 29.3333 23.3636 29.3333 16C29.3333 8.6364 23.3636 2.6667 16 2.6667Z' },
  { d: 'M16 10.6667V16' },
  { d: 'M16 21.3333H16.0133' },
];

@Component({
  selector: 'app-revisor',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './revisor.html',
  styleUrls: ['./revisor.scss']
})
export class RevisorComponent implements OnInit {

  cards: DashCard[] = [];

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void { this.construirCards(); }

  private construirCards(): void {
    const modulo = this.authService.getModulo();
    if (!modulo?.opciones?.length) { this.cards = []; return; }

    this.cards = modulo.opciones.map(op => {
      const rutaKey = (op.ruta || '').replace(/^\//, '').split('/').pop() ?? '';
      return {
        titulo:      op.nombre,
        descripcion: op.descripcion || '',
        ruta:        rutaKey,
        svgPaths:    SVG_MAP[rutaKey] ?? SVG_FALLBACK,
      };
    });
  }

  navegarA(ruta: string): void {
    this.router.navigateByUrl('/revisor/' + ruta);
  }
}
