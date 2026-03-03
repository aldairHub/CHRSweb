import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';

// =====================================================
// INTERFACES
// =====================================================
export interface ItemRubrica {
  id: string;
  label: string;
  max: number;
  puntosPor?: string; // ej. "1 punto por año"
}

export interface SeccionRubrica {
  codigo: string;
  titulo: string;
  descripcion: string;
  maximo: number;
  items: ItemRubrica[];
}

export interface AccionAfirmativa {
  id: string;
  label: string;
  puntos: number;
}

export interface Candidato {
  id: number;
  nombres: string;
  apellidos: string;
  carrera: string;
  titulos: string;
  puntajes: { [key: string]: number };
  accionesAfirmativas: { [key: string]: boolean };
  totalMerecimientos: number;
  totalExperienciaEntrevista: number;
  totalAccionAfirmativa: number;
  puntajeTotal: number;
}

@Component({
  selector: 'app-evaluacion-meritos',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, FooterComponent],
  templateUrl: './evaluacion.html',
  styleUrls: ['./evaluacion.scss']
})
export class EvaluacionMeritosComponent implements OnInit {

  // ====================================================
  // CANDIDATOS (3 - la terna oficial)
  // ====================================================
  candidatos: Candidato[] = [
    {
      id: 1,
      nombres: 'TORRES LINDAO',
      apellidos: 'VALERIA DAYANNA',
      carrera: 'Ingeniería en Sistemas',
      titulos: 'Master en Cyberseguridad',
      puntajes: {},
      accionesAfirmativas: {},
      totalMerecimientos: 0,
      totalExperienciaEntrevista: 0,
      totalAccionAfirmativa: 0,
      puntajeTotal: 0
    },
    {
      id: 2,
      nombres: 'ALMEIDA MURILLO',
      apellidos: 'JEAN CARLOS',
      carrera: 'Ingeniería en Sistemas',
      titulos: 'Maestría en Ciencia de Datos',
      puntajes: {},
      accionesAfirmativas: {},
      totalMerecimientos: 0,
      totalExperienciaEntrevista: 0,
      totalAccionAfirmativa: 0,
      puntajeTotal: 0
    },
    {
      id: 3,
      nombres: 'GUANÍN FAJARDO',
      apellidos: 'JORGE HUMBERTO',
      carrera: 'Ingeniero en Informática',
      titulos: 'MSc. Ciencia de Datos / PhD TIC',
      puntajes: {},
      accionesAfirmativas: {},
      totalMerecimientos: 0,
      totalExperienciaEntrevista: 0,
      totalAccionAfirmativa: 0,
      puntajeTotal: 0
    }
  ];

  // ====================================================
  // MATRIZ DE MÉRITOS — Arts. 15 y 16 de la Normativa UTEQ
  // ====================================================
  rubrica: SeccionRubrica[] = [
    {
      codigo: 'A',
      titulo: 'A) Título de cuarto nivel (maestría)',
      descripcion: 'Vinculada al campo amplio de la asignatura motivo del concurso',
      maximo: 20,
      items: [
        { id: 'a1', label: 'Título de maestría o superior verificado en SENESCYT', max: 20 }
      ]
    },
    {
      codigo: 'B',
      titulo: 'B) Experiencia en docencia, investigación, vinculación o gestión',
      descripcion: 'Experiencia universitaria o politécnica',
      maximo: 10,
      items: [
        { id: 'b1', label: 'Docencia universitaria en el área del concurso', max: 10, puntosPor: '1 punto por año' },
        { id: 'b2', label: 'Participación en proyectos de investigación ejecutados', max: 10, puntosPor: '1 punto por proyecto' },
        { id: 'b3', label: 'Participación en proyectos de vinculación ejecutados', max: 10, puntosPor: '1 punto por proyecto' },
        { id: 'b4', label: 'Actividades de gestión académica (Coordinadores/áreas)', max: 10, puntosPor: '1 punto por año' }
      ]
    },
    {
      codigo: 'C',
      titulo: 'C) Publicaciones en el área afín del concurso',
      descripcion: 'Producción científica verificable',
      maximo: 6,
      items: [
        { id: 'c1', label: 'Libros publicados', max: 6, puntosPor: '2 puntos por libro' },
        { id: 'c2', label: 'Artículos en revistas indexadas regionales', max: 6, puntosPor: '2 puntos por artículo' },
        { id: 'c3', label: 'Artículos en revistas indexadas con factor de impacto', max: 6, puntosPor: '4 puntos por artículo' }
      ]
    },
    {
      codigo: 'D',
      titulo: 'D) Cursos de actualización o perfeccionamiento profesional',
      descripcion: 'En los últimos cinco años — mínimo 30 horas',
      maximo: 10,
      items: [
        { id: 'd1', label: 'Asistencia a cursos (mín. 30 horas)', max: 10, puntosPor: '0.25 puntos por curso' },
        { id: 'd2', label: 'Cursos aprobados (mín. 30 horas)', max: 10, puntosPor: '0.5 puntos por curso' },
        { id: 'd3', label: 'Cursos impartidos (mín. 30 horas)', max: 10, puntosPor: '1 punto por curso' }
      ]
    },
    {
      codigo: 'E',
      titulo: 'E) Reconocimientos académicos de grado o posgrado',
      descripcion: 'Distinciones académicas obtenidas',
      maximo: 4,
      items: [
        { id: 'e1', label: 'Mejor graduado (grado o posgrado)', max: 2, puntosPor: '2 puntos' },
        { id: 'e2', label: 'Primer lugar en eventos académicos internacionales', max: 2, puntosPor: '2 puntos' },
        { id: 'e3', label: 'Primer lugar en eventos académicos nacionales', max: 1, puntosPor: '1 punto' },
        { id: 'e4', label: 'Evaluación ≥80% en último período como docente UTEQ o institución previa', max: 2, puntosPor: '2 puntos' }
      ]
    }
  ];

  // ====================================================
  // EXPERIENCIA + ENTREVISTA (50 puntos)
  // ====================================================
  bloqueExperiencia = [
    { id: 'exp_docencia', label: 'Experiencia profesional en la docencia', max: 15 },
    { id: 'exp_area', label: 'Experiencia profesional en el área de formación', max: 10 },
    { id: 'entrevista', label: 'Entrevista con la autoridad académica', max: 25 }
  ];

  // ====================================================
  // ACCIÓN AFIRMATIVA — Art. 17 de la Normativa UTEQ
  // ====================================================
  accionesAfirmativas: AccionAfirmativa[] = [
    { id: 'af_a', label: 'Ecuatoriana/o en el exterior ≥3 años (certificado consulado)', puntos: 2 },
    { id: 'af_b', label: 'Persona con discapacidad (certificado CONADIS/MSP)', puntos: 2 },
    { id: 'af_c', label: 'Domicilio en zona rural últimos 5 años (cert. junta parroquial)', puntos: 2 },
    { id: 'af_d', label: 'Quintiles 1 y 2 de pobreza (cert. Ministerio)', puntos: 2 },
    { id: 'af_e', label: 'Menor de 30 años o mayor de 65 años al postular', puntos: 2 },
    { id: 'af_f', label: 'Comunidad, pueblo o nacionalidad indígena / afroecuatoriana / montubia', puntos: 2 },
    { id: 'af_g', label: 'Pertenecer al sexo femenino', puntos: 2 },
    { id: 'af_h', label: 'Autoidentificación con géneros tradicionalmente excluidos', puntos: 2 },
    { id: 'af_i', label: 'Madre soltera y jefe de hogar (declaración juramentada)', puntos: 2 }
  ];

  // ====================================================
  // UI STATE
  // ====================================================
  mostrarAccionAfirmativa = false;
  guardando = false;
  guardado = false;
  mostrarModalGuardado = false;
  candidatoGanador: Candidato | null = null;

  // ====================================================
  // MODAL EDITAR CANDIDATO
  // ====================================================
  mostrarModalCandidato = false;
  candidatoEditando: Partial<Candidato> = {};
  candidatoIdx = -1;

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.inicializarPuntajes();
  }

  // ====================================================
  // INICIALIZACIÓN
  // ====================================================
  inicializarPuntajes(): void {
    this.candidatos.forEach(c => {
      // Méritos
      this.rubrica.forEach(seccion => {
        seccion.items.forEach(item => {
          if (c.puntajes[item.id] === undefined) c.puntajes[item.id] = 0;
        });
      });
      // Experiencia + Entrevista
      this.bloqueExperiencia.forEach(b => {
        if (c.puntajes[b.id] === undefined) c.puntajes[b.id] = 0;
      });
      // Acción afirmativa
      this.accionesAfirmativas.forEach(af => {
        if (c.accionesAfirmativas[af.id] === undefined) c.accionesAfirmativas[af.id] = false;
      });
      this.recalcular(c);
    });
  }

  // ====================================================
  // CÁLCULO (con límites por sección según normativa)
  // ====================================================
  recalcular(c: Candidato): void {
    // 1) Méritos (máx 50)
    const totales: { [codigo: string]: number } = {};
    this.rubrica.forEach(sec => {
      let sumaSeccion = 0;
      sec.items.forEach(item => {
        sumaSeccion += Number(c.puntajes[item.id] || 0);
      });
      totales[sec.codigo] = Math.min(sumaSeccion, sec.maximo);
    });
    c.totalMerecimientos = Math.min(
      Object.values(totales).reduce((a, b) => a + b, 0),
      50
    );

    // 2) Experiencia + Entrevista (máx 50)
    c.totalExperienciaEntrevista = Math.min(
      this.bloqueExperiencia.reduce((sum, b) => sum + Math.min(Number(c.puntajes[b.id] || 0), b.max), 0),
      50
    );

    // 3) Acción afirmativa (máx 4)
    let totalAf = 0;
    this.accionesAfirmativas.forEach(af => {
      if (c.accionesAfirmativas[af.id]) totalAf += af.puntos;
    });
    c.totalAccionAfirmativa = Math.min(totalAf, 4);

    // 4) Total general
    c.puntajeTotal = c.totalMerecimientos + c.totalExperienciaEntrevista + c.totalAccionAfirmativa;
  }

  // Subtotal por sección (con límite)
  subtotalSeccion(c: Candidato, sec: SeccionRubrica): number {
    const suma = sec.items.reduce((s, item) => s + Number(c.puntajes[item.id] || 0), 0);
    return Math.min(suma, sec.maximo);
  }

  // Subtotal bloque experiencia por fila
  valorExperiencia(c: Candidato, id: string, max: number): number {
    return Math.min(Number(c.puntajes[id] || 0), max);
  }

  // ====================================================
  // EDITAR CANDIDATO
  // ====================================================
  abrirEditarCandidato(idx: number): void {
    this.candidatoIdx = idx;
    this.candidatoEditando = { ...this.candidatos[idx] };
    this.mostrarModalCandidato = true;
  }

  guardarCandidato(): void {
    if (this.candidatoIdx >= 0) {
      this.candidatos[this.candidatoIdx] = {
        ...this.candidatos[this.candidatoIdx],
        nombres: this.candidatoEditando.nombres || '',
        apellidos: this.candidatoEditando.apellidos || '',
        carrera: this.candidatoEditando.carrera || '',
        titulos: this.candidatoEditando.titulos || ''
      };
    }
    this.mostrarModalCandidato = false;
  }

  // ====================================================
  // GUARDAR EVALUACIÓN
  // ====================================================
  guardarEvaluacion(): void {
    this.guardando = true;
    // Determinar ganador
    this.candidatoGanador = this.candidatos.reduce((prev, curr) =>
      curr.puntajeTotal > prev.puntajeTotal ? curr : prev
    );
    setTimeout(() => {
      this.guardando = false;
      this.guardado = true;
      this.mostrarModalGuardado = true;
    }, 800);
  }

  cerrarModalGuardado(): void {
    this.mostrarModalGuardado = false;
  }

  volver(): void {
    this.router.navigate(['/evaluador']);
  }

  // ====================================================
  // HELPERS
  // ====================================================
  trackById(_: number, item: any): number { return item.id; }
}
