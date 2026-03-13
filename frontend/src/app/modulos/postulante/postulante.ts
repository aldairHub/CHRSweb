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

  'subir-documentos': [
    { d: 'M19.9999 2.66675H7.99992C7.29267 2.66675 6.6144 2.9477 6.1143 3.4478C5.6142 3.94789 5.33325 4.62617 5.33325 5.33341V26.6667C5.33325 27.374 5.6142 28.0523 6.1143 28.5524C6.6144 29.0525 7.29267 29.3334 7.99992 29.3334H23.9999C24.7072 29.3334 25.3854 29.0525 25.8855 28.5524C26.3856 28.0523 26.6666 27.374 26.6666 26.6667V9.33341L19.9999 2.66675Z' },
    { d: 'M18.6667 2.66675V8.00008C18.6667 8.70733 18.9477 9.3856 19.4478 9.8857C19.9479 10.3858 20.6262 10.6667 21.3334 10.6667H26.6667' },
    { d: 'M13.3334 12H10.6667' },
    { d: 'M21.3334 17.3333H10.6667' },
    { d: 'M21.3334 22.6667H10.6667' },
  ],

  'resultados': [
    { d: 'M4 4V25.3333C4 26.0406 4.28095 26.7189 4.78105 27.219C5.28115 27.719 5.95942 28 6.66667 28H28' },
    { d: 'M24 22.6667V12' },
    { d: 'M17.3333 22.6667V6.66675' },
    { d: 'M10.6667 22.6667V18.6667' },
  ],

  'entrevista': [
    { d: 'M4 5.33333C4 4.62609 4.28095 3.94781 4.78105 3.44772C5.28115 2.94762 5.95942 2.66666 6.66667 2.66666H25.3333C26.0406 2.66666 26.7189 2.94762 27.219 3.44772C27.719 3.94781 28 4.62609 28 5.33333V18.6667C28 19.3739 27.719 20.0522 27.219 20.5523C26.7189 21.0524 26.0406 21.3333 25.3333 21.3333H12L6.66667 26.6667V21.3333H6.66667C5.95942 21.3333 5.28115 21.0524 4.78105 20.5523C4.28095 20.0522 4 19.3739 4 18.6667V5.33333Z' },
    { d: 'M10 10.6667H22' },
    { d: 'M10 15.3333H18' },
  ],

  'entrevistas': [
    { d: 'M4 5.33333C4 4.62609 4.28095 3.94781 4.78105 3.44772C5.28115 2.94762 5.95942 2.66666 6.66667 2.66666H25.3333C26.0406 2.66666 26.7189 2.94762 27.219 3.44772C27.719 3.94781 28 4.62609 28 5.33333V18.6667C28 19.3739 27.719 20.0522 27.219 20.5523C26.7189 21.0524 26.0406 21.3333 25.3333 21.3333H12L6.66667 26.6667V21.3333H6.66667C5.95942 21.3333 5.28115 21.0524 4.78105 20.5523C4.28095 20.0522 4 19.3739 4 18.6667V5.33333Z' },
    { d: 'M10 10.6667H22' },
    { d: 'M10 15.3333H18' },
  ],
};

const SVG_FALLBACK: Array<{ d: string }> = [
  { d: 'M16 2.6667C8.6364 2.6667 2.6667 8.6364 2.6667 16C2.6667 23.3636 8.6364 29.3333 16 29.3333C23.3636 29.3333 29.3333 23.3636 29.3333 16C29.3333 8.6364 23.3636 2.6667 16 2.6667Z' },
  { d: 'M16 10.6667V16' },
  { d: 'M16 21.3333H16.0133' },
];

@Component({
  selector: 'app-postulante',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './postulante.html',
  styleUrls: ['./postulante.scss']
})
export class PostulanteComponent implements OnInit {

  cards: DashCard[] = [];

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.construirCards();
  }

  private construirCards(): void {
    const modulo = this.authService.getModulo();
    if (!modulo?.opciones?.length) {
      // Fallback con las cards originales si no hay opciones en sesión
      this.cards = [
        {
          titulo: 'Subir Documentos',
          descripcion: 'Gestión segura de documentación',
          ruta: 'subir-documentos',
          svgPaths: SVG_MAP['subir-documentos'],
        },
        {
          titulo: 'Resultados',
          descripcion: 'Resultados de méritos',
          ruta: 'resultados',
          svgPaths: SVG_MAP['resultados'],
        },
        {
          titulo: 'Entrevista',
          descripcion: 'Gestión de entrevistas',
          ruta: 'entrevista',
          svgPaths: SVG_MAP['entrevista'],
        },
      ];
      return;
    }

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
    this.router.navigate([`/postulante/${ruta}`]);
  }
}
