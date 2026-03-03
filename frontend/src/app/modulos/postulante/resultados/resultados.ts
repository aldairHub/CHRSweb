import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';

interface SeccionResultado {
  nombre: string;
  puntaje: number;
  maximo: number;
  detalle: { criterio: string; puntaje: number; maximo: number }[];
}

@Component({
  selector: 'app-resultados',
  standalone: true,
  imports: [CommonModule, NavbarComponent, FooterComponent],
  templateUrl: './resultados.html',
  styleUrls: ['./resultados.scss']
})
export class ResultadosComponent implements OnInit {

  tabActiva: 'resumen' | 'meritos' | 'entrevista' | 'observaciones' = 'resumen';

  estado: 'en_proceso' | 'aprobado' | 'rechazado' = 'aprobado';

  postulante = {
    nombre: 'Juan Carlos Pérez Loor',
    cedula: '1205489632',
    proceso: 'Docente No Titular — Área: Tecnologías de la Información',
    fecha: '14/05/2025'
  };

  resultados = {
    totalMeritos: 24,
    totalExperiencia: 38,
    totalAccionAfirmativa: 2,
    total: 64,
    posicion: 2,
    totalCandidatos: 3
  };

  seccionesMeritos: SeccionResultado[] = [
    {
      nombre: 'A) Título de cuarto nivel',
      puntaje: 20,
      maximo: 20,
      detalle: [
        { criterio: 'Título de maestría verificado en SENESCYT', puntaje: 20, maximo: 20 }
      ]
    },
    {
      nombre: 'B) Experiencia docente/investigación',
      puntaje: 2,
      maximo: 10,
      detalle: [
        { criterio: 'Docencia universitaria', puntaje: 2, maximo: 10 },
        { criterio: 'Proyectos de investigación', puntaje: 0, maximo: 10 },
        { criterio: 'Gestión académica', puntaje: 0, maximo: 10 }
      ]
    },
    {
      nombre: 'C) Publicaciones',
      puntaje: 0,
      maximo: 6,
      detalle: [
        { criterio: 'Libros publicados', puntaje: 0, maximo: 6 },
        { criterio: 'Artículos en revistas indexadas', puntaje: 0, maximo: 6 }
      ]
    },
    {
      nombre: 'D) Cursos de actualización',
      puntaje: 2,
      maximo: 10,
      detalle: [
        { criterio: 'Cursos aprobados (0.5 pts c/u)', puntaje: 2, maximo: 10 }
      ]
    },
    {
      nombre: 'E) Reconocimientos académicos',
      puntaje: 0,
      maximo: 4,
      detalle: [
        { criterio: 'Reconocimientos registrados', puntaje: 0, maximo: 4 }
      ]
    }
  ];

  seccionesExperiencia: SeccionResultado[] = [
    {
      nombre: 'Experiencia en docencia',
      puntaje: 15,
      maximo: 15,
      detalle: [{ criterio: 'Experiencia profesional en docencia', puntaje: 15, maximo: 15 }]
    },
    {
      nombre: 'Experiencia en el área',
      puntaje: 10,
      maximo: 10,
      detalle: [{ criterio: 'Experiencia en área de formación', puntaje: 10, maximo: 10 }]
    },
    {
      nombre: 'Entrevista y clase demostrativa',
      puntaje: 13,
      maximo: 25,
      detalle: [
        { criterio: 'Vocación Docente', puntaje: 4, maximo: 5 },
        { criterio: 'Actitud asertiva', puntaje: 2, maximo: 2 },
        { criterio: 'Habilidades sociales y comunicativas', puntaje: 1, maximo: 2 },
        { criterio: 'Compromiso y responsabilidad', puntaje: 2, maximo: 2 },
        { criterio: 'Autenticidad y autoimagen', puntaje: 1, maximo: 2 },
        { criterio: 'Flexibilidad cognitiva', puntaje: 1, maximo: 2 },
        { criterio: 'Habilidades pedagógicas', puntaje: 1, maximo: 2 },
        { criterio: 'Dominio del contenido', puntaje: 1, maximo: 2 }
      ]
    }
  ];

  observaciones = 'El candidato demostró un buen nivel de conocimientos en el área de formación. Sin embargo, se recomienda fortalecer la experiencia en investigación y publicaciones científicas para futuras postulaciones.';

  get porcentajeTotal(): number {
    return Math.round((this.resultados.total / 104) * 100);
  }

  get colorEstado(): string {
    if (this.estado === 'aprobado') return 'verde';
    if (this.estado === 'rechazado') return 'rojo';
    return 'amarillo';
  }

  constructor(private router: Router) {}

  ngOnInit(): void {}

  getBarWidth(puntaje: number, maximo: number): number {
    return Math.round((puntaje / maximo) * 100);
  }

  volver(): void {
    this.router.navigate(['/postulante']);
  }
}
