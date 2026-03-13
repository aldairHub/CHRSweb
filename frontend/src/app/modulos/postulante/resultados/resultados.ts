import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DocumentoService } from '../../../services/documento.service';

interface SeccionResultado {
  nombre: string;
  puntaje: number;
  maximo: number;
  detalle: { criterio: string; puntaje: number; maximo: number }[];
}

@Component({
  selector: 'app-resultados',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './resultados.html',
  styleUrls: ['./resultados.scss']
})
export class ResultadosComponent implements OnInit {

  tabActiva: 'resumen' | 'meritos' | 'entrevista' | 'observaciones' = 'resumen';
  cargando = true;
  sinDatos = false;

  estado: 'en_proceso' | 'aprobado' | 'rechazado' = 'en_proceso';

  postulante = { nombre: '', cedula: '', proceso: '', fecha: '' };

  resultados = {
    totalMeritos: 0, totalExperiencia: 0,
    totalAccionAfirmativa: 0, total: 0,
    posicion: 0, totalCandidatos: 0
  };

  seccionesMeritos: SeccionResultado[] = [];
  seccionesExperiencia: SeccionResultado[] = [];
  observaciones = '';

  constructor(private router: Router, private documentoSvc: DocumentoService) {}

  ngOnInit(): void {
    const idUsuario = Number(localStorage.getItem('idUsuario'));
    if (!idUsuario) { this.router.navigate(['/login']); return; }

    this.documentoSvc.obtenerResultadosPostulante(idUsuario).subscribe({
      next: data => {
        if (!data || !data.puntajes) { this.sinDatos = true; this.cargando = false; return; }
        this.mapearDatos(data);
        this.cargando = false;
      },
      error: () => { this.sinDatos = true; this.cargando = false; }
    });
  }

  private mapearDatos(data: any): void {
    const p = data.puntajes as Record<string, number>;

    const g = (key: string) => p[key] ?? 0;

    // Estado
    const est = data.estado_general ?? 'en_proceso';
    this.estado = est === 'completado' ? 'aprobado' : est === 'rechazado' ? 'rechazado' : 'en_proceso';

    this.observaciones = data.justificacion_decision ?? 'Sin observaciones registradas.';

    // Méritos
    const merA = g('a1');
    const merB = g('b1') + g('b2') + g('b3') + g('b4');
    const merC = g('c1') + g('c2') + g('c3');
    const merD = g('d1') + g('d2') + g('d3');
    const merE = g('e1') + g('e2') + g('e3') + g('e4');
    this.resultados.totalMeritos = merA + merB + merC + merD + merE;

    // Experiencia + entrevista
    const expDoc = g('exp_docencia');
    const expArea = g('exp_area');
    const entrevista = g('entrevista');
    this.resultados.totalExperiencia = expDoc + expArea + entrevista;

    // Acción afirmativa
    const af = ['af_a','af_b','af_c','af_d','af_e','af_f','af_g','af_h','af_i']
      .reduce((sum, k) => sum + g(k), 0);
    this.resultados.totalAccionAfirmativa = af;

    this.resultados.total = this.resultados.totalMeritos +
      this.resultados.totalExperiencia +
      this.resultados.totalAccionAfirmativa;

    // Secciones méritos
    this.seccionesMeritos = [
      { nombre: 'A) Título de cuarto nivel', puntaje: merA, maximo: 20,
        detalle: [{ criterio: 'Título verificado en SENESCYT', puntaje: g('a1'), maximo: 20 }] },
      { nombre: 'B) Experiencia docente/investigación', puntaje: merB, maximo: 10,
        detalle: [
          { criterio: 'Docencia universitaria', puntaje: g('b1'), maximo: 10 },
          { criterio: 'Proyectos de investigación', puntaje: g('b2'), maximo: 10 },
          { criterio: 'Gestión académica', puntaje: g('b3'), maximo: 10 },
          { criterio: 'Otros', puntaje: g('b4'), maximo: 10 }
        ]},
      { nombre: 'C) Publicaciones', puntaje: merC, maximo: 6,
        detalle: [
          { criterio: 'Libros publicados', puntaje: g('c1'), maximo: 6 },
          { criterio: 'Artículos indexados', puntaje: g('c2'), maximo: 6 },
          { criterio: 'Otros', puntaje: g('c3'), maximo: 6 }
        ]},
      { nombre: 'D) Cursos de actualización', puntaje: merD, maximo: 10,
        detalle: [
          { criterio: 'Cursos aprobados', puntaje: g('d1'), maximo: 10 },
          { criterio: 'Seminarios', puntaje: g('d2'), maximo: 10 },
          { criterio: 'Otros', puntaje: g('d3'), maximo: 10 }
        ]},
      { nombre: 'E) Reconocimientos académicos', puntaje: merE, maximo: 4,
        detalle: [
          { criterio: 'Reconocimientos', puntaje: g('e1'), maximo: 4 },
          { criterio: 'Premios', puntaje: g('e2'), maximo: 4 },
          { criterio: 'Distinciones', puntaje: g('e3'), maximo: 4 },
          { criterio: 'Otros', puntaje: g('e4'), maximo: 4 }
        ]}
    ];

    // Secciones experiencia
    this.seccionesExperiencia = [
      { nombre: 'Experiencia en docencia', puntaje: expDoc, maximo: 15,
        detalle: [{ criterio: 'Experiencia profesional en docencia', puntaje: expDoc, maximo: 15 }] },
      { nombre: 'Experiencia en el área', puntaje: expArea, maximo: 10,
        detalle: [{ criterio: 'Experiencia en área de formación', puntaje: expArea, maximo: 10 }] },
      { nombre: 'Entrevista y clase demostrativa', puntaje: entrevista, maximo: 25,
        detalle: [{ criterio: 'Puntaje de entrevista', puntaje: entrevista, maximo: 25 }] }
    ];
  }

  get porcentajeTotal(): number {
    return Math.round((this.resultados.total / 104) * 100);
  }

  get colorEstado(): string {
    if (this.estado === 'aprobado') return 'verde';
    if (this.estado === 'rechazado') return 'rojo';
    return 'amarillo';
  }

  getBarWidth(puntaje: number, maximo: number): number {
    if (!maximo) return 0;
    return Math.round((puntaje / maximo) * 100);
  }

  volver(): void {
    this.router.navigate(['/postulante']);
  }
}
