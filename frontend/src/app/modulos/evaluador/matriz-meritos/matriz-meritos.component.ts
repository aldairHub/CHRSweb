import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';

export interface ItemRubrica {
  id: string;
  label: string;
  max: number;
  puntosPor?: string;
  bloqueado?: boolean;
}

export interface SeccionRubrica {
  idSeccion: number;
  codigo: string;
  titulo: string;
  descripcion: string;
  maximo: number;
  tipo: string;
  bloqueado: boolean;
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
  totalExperiencia: number;
  totalEntrevista: number;
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

  private readonly API     = 'http://localhost:8080/api/matriz-meritos';
  private readonly API_CFG = 'http://localhost:8080/api/matriz-config';

  readonly PUNTAJE_MINIMO = 50; // mínimo sobre 100 para pasar a entrevistas

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

  // Estructura cargada desde BD
  seccionesMeritos: SeccionRubrica[] = [];
  seccionesExperiencia: SeccionRubrica[] = [];
  seccionEntrevista: SeccionRubrica | null = null;
  accionesAfirmativas: AccionAfirmativa[] = [];

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['idSolicitud']) {
        this.idConvocatoria = +params['idSolicitud'];
        this.cargarTodo();
      }
    });
  }

  cargarTodo(): void {
    this.cargando = true;
    this.error = '';

    forkJoin({
      estructura: this.http.get<any>(`${this.API_CFG}/estructura`),
      matriz:     this.http.get<any>(`${this.API}/solicitud/${this.idConvocatoria}`)
    }).subscribe({
      next: ({ estructura, matriz }) => {
        this.procesarEstructura(estructura);
        this.procesarCandidatos(matriz);
        this.cargando = false;
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error al cargar los datos.';
        this.cargando = false;
      }
    });
  }

  private procesarEstructura(estructura: any): void {
    const secciones: SeccionRubrica[] = (estructura.secciones || []).map((s: any) => ({
      idSeccion:   s.idSeccion,
      codigo:      s.codigo,
      titulo:      s.titulo,
      descripcion: s.descripcion,
      maximo:      s.puntajeMaximo,
      tipo:        s.tipo,
      bloqueado:   s.bloqueado,
      items:       (s.items || []).map((i: any) => ({
        id:        i.codigo,
        label:     i.label,
        max:       i.puntajeMaximo,
        puntosPor: i.puntosPor,
        bloqueado: i.bloqueado
      }))
    }));

    this.seccionesMeritos    = secciones.filter(s => s.tipo === 'meritos');
    this.seccionesExperiencia = secciones.filter(s => s.tipo === 'experiencia');
    this.seccionEntrevista   = secciones.find(s => s.tipo === 'entrevista') || null;

    this.accionesAfirmativas = (estructura.accionesAfirmativas || []).map((a: any) => ({
      id:     a.codigo,
      label:  a.label,
      puntos: a.puntos
    }));
  }

  private procesarCandidatos(matriz: any): void {
    this.convocatoriaInfo = matriz.convocatoria;
    this.candidatos = (matriz.candidatos || []).map((c: any) => ({
      id:                  c.idPostulante,
      idProceso:           c.idProceso,
      idSolicitud:         c.idSolicitud,
      nombres:             c.nombres,
      apellidos:           c.apellidos,
      titulos:             c.titulos || '',
      puntajes:            c.puntajes || {},
      accionesAfirmativas: c.accionesAfirmativas || {},
      totalMerecimientos:  0,
      totalExperiencia:    0,
      totalEntrevista:     0,
      totalAccionAfirmativa: 0,
      puntajeTotal:        0,
      habilitadoEntrevista: c.habilitadoEntrevista || false
    }));

    this.inicializarPuntajes();
    if (this.candidatos.some(c => c.puntajeTotal > 0)) {
      this.guardado = true;
    }
  }

  inicializarPuntajes(): void {
    const todasLasSecciones = [
      ...this.seccionesMeritos,
      ...this.seccionesExperiencia,
      ...(this.seccionEntrevista ? [this.seccionEntrevista] : [])
    ];

    this.candidatos.forEach(c => {
      todasLasSecciones.forEach(sec => {
        sec.items.forEach(item => {
          if (c.puntajes[item.id] === undefined) c.puntajes[item.id] = 0;
        });
      });
      this.accionesAfirmativas.forEach(af => {
        if (c.accionesAfirmativas[af.id] === undefined) c.accionesAfirmativas[af.id] = false;
      });
      this.recalcular(c);
    });
  }

  recalcular(c: Candidato): void {
    // 1. Méritos (máx 50)
    let totalMeritos = 0;
    this.seccionesMeritos.forEach(sec => {
      const sumaSeccion = sec.items.reduce((s, item) => s + Number(c.puntajes[item.id] || 0), 0);
      totalMeritos += Math.min(sumaSeccion, sec.maximo);
    });
    c.totalMerecimientos = Math.min(totalMeritos, 50);

    // 2. Experiencia (máx 25)
    let totalExp = 0;
    this.seccionesExperiencia.forEach(sec => {
      const sumaSeccion = sec.items.reduce((s, item) => s + Math.min(Number(c.puntajes[item.id] || 0), item.max), 0);
      totalExp += Math.min(sumaSeccion, sec.maximo);
    });
    c.totalExperiencia = Math.min(totalExp, 25);

    // 3. Entrevista (máx 25 — bloqueado, viene de entrevistas docentes)
    c.totalEntrevista = this.seccionEntrevista
      ? Math.min(Number(c.puntajes['entrevista'] || 0), 25)
      : 0;

    // 4. Acción afirmativa (bonificación, máx 4)
    let totalAf = 0;
    this.accionesAfirmativas.forEach(af => { if (c.accionesAfirmativas[af.id]) totalAf += af.puntos; });
    c.totalAccionAfirmativa = Math.min(totalAf, 4);

    // 5. Total sobre 100 (+ bonificación)
    c.puntajeTotal = c.totalMerecimientos + c.totalExperiencia + c.totalEntrevista + c.totalAccionAfirmativa;
  }

  subtotalSeccion(c: Candidato, sec: SeccionRubrica): number {
    const suma = sec.items.reduce((s, item) => s + Number(c.puntajes[item.id] || 0), 0);
    return Math.min(suma, sec.maximo);
  }

  guardarEvaluacion(): void {
    if (this.convocatoriaInfo?.bloqueada) {
      alert('La matriz de méritos está bloqueada.');
      return;
    }
    this.guardando = true;

    const payload = {
      idConvocatoria: this.idConvocatoria,
      candidatos: this.candidatos.map(c => ({
        idProceso:            c.idProceso,
        idSolicitud:          c.idSolicitud,
        puntajes:             c.puntajes,
        accionesAfirmativas:  c.accionesAfirmativas,
        totalMerecimientos:   c.totalMerecimientos,
        totalExperiencia:     c.totalExperiencia,
        totalEntrevista:      c.totalEntrevista,
        totalAccionAfirmativa: c.totalAccionAfirmativa,
        puntajeTotal:         c.puntajeTotal
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
    this.candidatoOverride     = c;
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

  get todasLasSecciones(): SeccionRubrica[] {
    return [
      ...this.seccionesMeritos,
      ...this.seccionesExperiencia,
      ...(this.seccionEntrevista ? [this.seccionEntrevista] : [])
    ];
  }
}
