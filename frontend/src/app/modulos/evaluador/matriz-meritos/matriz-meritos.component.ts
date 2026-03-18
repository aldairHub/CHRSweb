import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';

export interface ItemRubrica {
  id: string;
  label: string;
  max: number;
  puntosPor?: string;
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
  idProceso: number;
  idSolicitud: number;
  nombres: string;
  apellidos: string;
  titulos: string;
  puntajes: { [key: string]: number };
  accionesAfirmativas: { [key: string]: boolean };
  totalMerecimientos: number;
  totalExperienciaEntrevista: number;
  totalAccionAfirmativa: number;
  puntajeTotal: number;
  habilitadoEntrevista: boolean;
}

export interface ConvocatoriaInfo {
  idConvocatoria: number;
  titulo: string;
  fechaLimiteDocumentos: string;
  materia: string;
  bloqueada: boolean;
  mensajeBloqueo?: string;
}

@Component({
  selector: 'app-matriz-meritos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './matriz-meritos.component.html',
  styleUrls: ['./matriz-meritos.component.scss']
})
export class MatrizMeritosComponent implements OnInit {

  private readonly API = 'http://localhost:8080/api/matriz-meritos';

  readonly PUNTAJE_MINIMO = 75;

  cargando = false;
  guardando = false;
  guardado = false;
  mostrarModalGuardado = false;
  mostrarAccionAfirmativa = false;
  error = '';

  mostrarModalOverride = false;
  candidatoOverride: Candidato | null = null;
  justificacionOverride = '';
  guardandoOverride = false;

  convocatoriaInfo: ConvocatoriaInfo | null = null;
  candidatos: Candidato[] = [];
  idConvocatoria = 0;

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

  bloqueExperiencia = [
    { id: 'exp_docencia', label: 'Experiencia profesional en la docencia', max: 15 },
    { id: 'exp_area',    label: 'Experiencia profesional en el área de formación', max: 10 },
    { id: 'entrevista',  label: 'Entrevista con la autoridad académica', max: 25 }
  ];

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

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['idSolicitud']) {
        this.idConvocatoria = +params['idSolicitud'];
        this.cargarDatos();
      }
    });
  }

  cargarDatos(): void {
    if (!this.idConvocatoria) return;
    this.cargando = true;
    this.error = '';

    this.http.get<any>(`${this.API}/solicitud/${this.idConvocatoria}`).subscribe({
      next: (data) => {
        this.convocatoriaInfo = data.convocatoria;
        this.candidatos = data.candidatos.map((c: any) => ({
          id:                  c.idPostulante,
          idProceso:           c.idProceso,
          idSolicitud:         c.idSolicitud,
          nombres:             c.nombres,
          apellidos:           c.apellidos,
          titulos:             c.titulos || '',
          puntajes:            c.puntajes || {},
          accionesAfirmativas: c.accionesAfirmativas || {},
          totalMerecimientos:        0,
          totalExperienciaEntrevista: 0,
          totalAccionAfirmativa:      0,
          puntajeTotal:              0,
          habilitadoEntrevista:      c.habilitadoEntrevista || false
        }));
        this.inicializarPuntajes();
        if (this.candidatos.some(c => c.puntajeTotal > 0)) {
          this.guardado = true;
        }
        this.cargando = false;
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error al cargar los datos de la convocatoria.';
        this.cargando = false;
      }
    });
  }

  inicializarPuntajes(): void {
    this.candidatos.forEach(c => {
      this.rubrica.forEach(seccion => {
        seccion.items.forEach(item => {
          if (c.puntajes[item.id] === undefined) c.puntajes[item.id] = 0;
        });
      });
      this.bloqueExperiencia.forEach(b => {
        if (c.puntajes[b.id] === undefined) c.puntajes[b.id] = 0;
      });
      this.accionesAfirmativas.forEach(af => {
        if (c.accionesAfirmativas[af.id] === undefined) c.accionesAfirmativas[af.id] = false;
      });
      this.recalcular(c);
    });
  }

  recalcular(c: Candidato): void {
    const totales: { [codigo: string]: number } = {};
    this.rubrica.forEach(sec => {
      let sumaSeccion = 0;
      sec.items.forEach(item => { sumaSeccion += Number(c.puntajes[item.id] || 0); });
      totales[sec.codigo] = Math.min(sumaSeccion, sec.maximo);
    });
    c.totalMerecimientos = Math.min(Object.values(totales).reduce((a, b) => a + b, 0), 50);

    c.totalExperienciaEntrevista = Math.min(
      this.bloqueExperiencia.reduce((sum, b) => sum + Math.min(Number(c.puntajes[b.id] || 0), b.max), 0), 50
    );

    let totalAf = 0;
    this.accionesAfirmativas.forEach(af => { if (c.accionesAfirmativas[af.id]) totalAf += af.puntos; });
    c.totalAccionAfirmativa = Math.min(totalAf, 4);

    c.puntajeTotal = c.totalMerecimientos + c.totalExperienciaEntrevista + c.totalAccionAfirmativa;
  }

  // ── Recalcular con límite de sección ────────────────────────
  recalcularConLimite(c: Candidato, itemId: string, seccion: SeccionRubrica): void {
    // Subtotal de la sección sin contar el item actual
    const subtotalSinItem = seccion.items
      .filter(i => i.id !== itemId)
      .reduce((s, i) => s + Number(c.puntajes[i.id] || 0), 0);

    // Cuánto queda disponible para este item
    const disponible = Math.max(0, seccion.maximo - subtotalSinItem);

    // Límite = mínimo entre el disponible y el máximo propio del item
    const itemMax = seccion.items.find(i => i.id === itemId)?.max ?? 0;
    const limite = Math.min(disponible, itemMax);

    // Recortar si supera el límite
    if (Number(c.puntajes[itemId]) > limite) {
      c.puntajes[itemId] = limite;
    }

    // También asegurar que no sea negativo
    if (Number(c.puntajes[itemId]) < 0) {
      c.puntajes[itemId] = 0;
    }

    this.recalcular(c);
  }

  subtotalSeccion(c: Candidato, sec: SeccionRubrica): number {
    const suma = sec.items.reduce((s, item) => s + Number(c.puntajes[item.id] || 0), 0);
    return Math.min(suma, sec.maximo);
  }

  guardarEvaluacion(): void {
    if (this.convocatoriaInfo?.bloqueada) {
      alert('La matriz de méritos está bloqueada. El período de documentos aún no ha cerrado.');
      return;
    }

    this.guardando = true;

    const payload = {
      idConvocatoria: this.idConvocatoria,
      candidatos: this.candidatos.map(c => ({
        idProceso:                  c.idProceso,
        idSolicitud:                c.idSolicitud,
        puntajes:                   c.puntajes,
        accionesAfirmativas:        c.accionesAfirmativas,
        totalMerecimientos:         c.totalMerecimientos,
        totalExperienciaEntrevista: c.totalExperienciaEntrevista,
        totalAccionAfirmativa:      c.totalAccionAfirmativa,
        puntajeTotal:               c.puntajeTotal
      }))
    };

    this.http.post(`${this.API}/guardar`, payload).subscribe({
      next: () => {
        this.guardando = false;
        this.guardado  = true;
        this.mostrarModalGuardado = true;
      },
      error: (err) => {
        alert(err?.error?.mensaje || 'Error al guardar la evaluación.');
        this.guardando = false;
      }
    });
  }

  abrirModalOverride(c: Candidato): void {
    this.candidatoOverride    = c;
    this.justificacionOverride = '';
    this.mostrarModalOverride  = true;
  }

  cerrarModalOverride(): void {
    if (this.guardandoOverride) return;
    this.mostrarModalOverride  = false;
    this.candidatoOverride     = null;
    this.justificacionOverride = '';
  }

  confirmarOverride(): void {
    if (!this.justificacionOverride.trim() || !this.candidatoOverride) return;
    this.guardandoOverride = true;

    this.http.post(`${this.API}/habilitar-entrevista`, {
      idProceso:     this.candidatoOverride.idProceso,
      justificacion: this.justificacionOverride.trim()
    }).subscribe({
      next: () => {
        this.candidatoOverride!.habilitadoEntrevista = true;
        this.guardandoOverride = false;
        this.cerrarModalOverride();
      },
      error: (err) => {
        alert(err?.error?.mensaje || 'Error al habilitar el candidato.');
        this.guardandoOverride = false;
      }
    });
  }

  cerrarModalGuardado(): void { this.mostrarModalGuardado = false; }

  volver(): void { this.router.navigate(['/evaluador/matriz-meritos']); }

  trackById(_: number, item: any): number { return item.id; }

  get matrizBloqueada(): boolean {
    return this.convocatoriaInfo?.bloqueada ?? false;
  }

  get hoy(): string {
    return new Date().toISOString().split('T')[0];
  }
}
