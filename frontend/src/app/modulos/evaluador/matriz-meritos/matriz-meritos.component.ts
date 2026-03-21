import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { ModalEvaluadoresComponent } from '../../../component/modal-evaluadores.component';
import { ToastService } from '../../../services/toast.service';
import { AuthStateService } from '../../../services/auth-state.service';

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
  // NUEVOS:
  bloqueado: boolean;
  motivoBloqueo: string | null;
  tieneDocumentos: boolean;
}

export interface CandidatoComite {
  id_proceso: number;
  nombres: string;
  apellidos: string;
  puntaje_matriz: number;
  puntaje_entrevista: number;
  puntaje_total: number;
  decision_comite: string;
  bloqueado: boolean;
}

export interface ConvocatoriaInfo {
  idConvocatoria: number;
  idSolicitud: number;
  titulo: string;
  fechaLimiteDocumentos: string;
  materia: string;
  bloqueada: boolean;
  mensajeBloqueo?: string;
}

@Component({
  selector: 'app-matriz-meritos',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalEvaluadoresComponent],
  templateUrl: './matriz-meritos.component.html',
  styleUrls: ['./matriz-meritos.component.scss']
})
export class MatrizMeritosComponent implements OnInit {

  private readonly API     = 'http://localhost:8080/api/matriz-meritos';
  private readonly API_CFG = 'http://localhost:8080/api/matriz-config';
  private readonly API_COMITE = 'http://localhost:8080/api/comite-final';

  readonly PUNTAJE_MINIMO = 50;

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

  // Modal evaluadores
  modalEvaluadoresVisible = false;
  candidatoEvaluadores: Candidato | null = null;

  // Comité final
  candidatosComite: CandidatoComite[] = [];
  cargandoComite = false;
  comiteConfirmado = false;
  idProcesoGanador: number | null = null;
  idProcesoGanadorOriginal: number | null = null;  // el de mayor puntaje al cargar
  actaComite = '';
  justificacionCambioGanador = '';                 // obligatorio si se cambia el ganador
  confirmandoComite = false;
  esSolicitanteComite = false;                     // solo el solicitante confirma
  notificando = false;
  notificado  = false;
  descargandoActa    = false;
  descargandoInforme = false;

  private readonly API_REPORTE = 'http://localhost:8080/api/comite-final';

  convocatoriaInfo: ConvocatoriaInfo | null = null;
  candidatos: Candidato[] = [];
  idConvocatoria = 0;

  seccionesMeritos: SeccionRubrica[] = []
  seccionesExperiencia: SeccionRubrica[] = [];
  seccionEntrevista: SeccionRubrica | null = null;
  accionesAfirmativas: AccionAfirmativa[] = [];

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private toast: ToastService,
    private authState: AuthStateService
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
        if (this.guardado) this.cargarComite();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error al cargar los datos.';
        this.cargando = false;
        this.cdr.detectChanges();
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

    this.seccionesMeritos     = secciones.filter(s => s.tipo === 'meritos');
    this.seccionesExperiencia = secciones.filter(s => s.tipo === 'experiencia');
    this.seccionEntrevista    = secciones.find(s => s.tipo === 'entrevista') || null;

    this.accionesAfirmativas = (estructura.accionesAfirmativas || []).map((a: any) => ({
      id:     a.codigo,
      label:  a.label,
      puntos: a.puntos
    }));
  }

  // REEMPLAZAR el método procesarCandidatos:
  private procesarCandidatos(matriz: any): void {
    this.convocatoriaInfo = matriz.convocatoria;
    this.candidatos = (matriz.candidatos || []).map((c: any) => ({
      id:                    c.idPostulante,
      idProceso:             c.idProceso,
      idSolicitud:           c.idSolicitud,
      nombres:               c.nombres,
      apellidos:             c.apellidos,
      titulos:               c.titulos || '',
      puntajes:              c.puntajes || {},
      accionesAfirmativas:   c.accionesAfirmativas || {},
      totalMerecimientos:    0,
      totalExperiencia:      0,
      totalEntrevista:       0,
      totalAccionAfirmativa: 0,
      puntajeTotal:          0,
      habilitadoEntrevista:  c.habilitadoEntrevista || false,
      // NUEVOS:
      bloqueado:             c.bloqueado || false,
      motivoBloqueo:         c.motivoBloqueo || null,
      tieneDocumentos:       c.tieneDocumentos !== false
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
    let totalMeritos = 0;
    this.seccionesMeritos.forEach(sec => {
      const suma = sec.items.reduce((s, item) => s + Number(c.puntajes[item.id] || 0), 0);
      totalMeritos += Math.min(suma, sec.maximo);
    });
    c.totalMerecimientos = Math.min(totalMeritos, 50);

    let totalExp = 0;
    this.seccionesExperiencia.forEach(sec => {
      const suma = sec.items.reduce((s, item) => s + Math.min(Number(c.puntajes[item.id] || 0), item.max), 0);
      totalExp += Math.min(suma, sec.maximo);
    });
    c.totalExperiencia = Math.min(totalExp, 25);

    c.totalEntrevista = this.seccionEntrevista
      ? Math.min(Number(c.puntajes['entrevista'] || 0), 25)
      : 0;

    let totalAf = 0;
    this.accionesAfirmativas.forEach(af => { if (c.accionesAfirmativas[af.id]) totalAf += af.puntos; });
    c.totalAccionAfirmativa = Math.min(totalAf, 4);

    c.puntajeTotal = c.totalMerecimientos + c.totalExperiencia + c.totalEntrevista + c.totalAccionAfirmativa;
  }

  subtotalSeccion(c: Candidato, sec: SeccionRubrica): number {
    const suma = sec.items.reduce((s, item) => s + Number(c.puntajes[item.id] || 0), 0);
    return Math.min(suma, sec.maximo);
  }

  guardarEvaluacion(): void {
    if (this.convocatoriaInfo?.bloqueada) {
      this.toast.warning('Matriz bloqueada', 'No se puede modificar una evaluación bloqueada.');
      return;
    }
    this.guardando = true;

    const payload = {
      idConvocatoria: this.idConvocatoria,
      candidatos: this.candidatos.map(c => ({
        idProceso:             c.idProceso,
        idSolicitud:           c.idSolicitud,
        puntajes:              c.puntajes,
        accionesAfirmativas:   c.accionesAfirmativas,
        totalMerecimientos:    c.totalMerecimientos,
        totalExperiencia:      c.totalExperiencia,
        totalEntrevista:       c.totalEntrevista,
        totalAccionAfirmativa: c.totalAccionAfirmativa,
        puntajeTotal:          c.puntajeTotal
      }))
    };

    this.http.post(`${this.API}/guardar`, payload).subscribe({
      next: () => {
        this.guardando = false;
        this.guardado  = true;
        this.mostrarModalGuardado = true;
        this.cargarComite();
      },
      error: (err) => {
        this.toast.error('Error al guardar', err?.error?.mensaje || 'No se pudo guardar la evaluación.');
        this.guardando = false;
      }
    });
  }

  // ── Comité Final ─────────────────────────────────────────────
  cargarComite(): void {
    if (!this.idConvocatoria) return;
    this.cargandoComite = true;

    // Verificar si el usuario logueado es el solicitante
    this.http.get<any>(`${this.API_COMITE}/solicitud/${this.idConvocatoria}/es-solicitante`).subscribe({
      next: (res) => { this.esSolicitanteComite = res.esSolicitante ?? false; }
    });

    this.http.get<any>(`${this.API_COMITE}/solicitud/${this.idConvocatoria}/confirmado`).subscribe({
      next: (res) => { this.comiteConfirmado = res.confirmado; }
    });

    this.http.get<CandidatoComite[]>(`${this.API_COMITE}/solicitud/${this.idConvocatoria}/candidatos`).subscribe({
      next: (data) => {
        this.candidatosComite = data;
        this.cargandoComite = false;

        if (this.comiteConfirmado) {
          // Ya confirmado: mostrar ganador guardado
          const ganador = data.find(c => c.decision_comite === 'ganador');
          if (ganador) this.idProcesoGanador = ganador.id_proceso;
        } else if (data.length > 0) {
          // Aún no confirmado: auto-seleccionar el de mayor puntaje (viene ordenado DESC)
          this.idProcesoGanador         = data[0].id_proceso;
          this.idProcesoGanadorOriginal = data[0].id_proceso;
          this.justificacionCambioGanador = '';
        }
        this.cdr.detectChanges();
      },
      error: () => { this.cargandoComite = false; }
    });
  }

  // Getters para el panel comité
  get ganadorCambiado(): boolean {
    return this.idProcesoGanador !== null
      && this.idProcesoGanadorOriginal !== null
      && this.idProcesoGanador !== this.idProcesoGanadorOriginal;
  }

  get todosConEntrevista(): boolean {
    return this.candidatosComite.length > 0
      && this.candidatosComite.every(c => (c.puntaje_entrevista ?? 0) > 0);
  }

  get puedeConfirmarComite(): boolean {
    if (!this.esSolicitanteComite)  return false;
    if (!this.todosConEntrevista)   return false;
    if (!this.idProcesoGanador)     return false;
    if (!this.actaComite.trim())    return false;
    if (this.ganadorCambiado && !this.justificacionCambioGanador.trim()) return false;
    return true;
  }

  confirmarDecisionComite(): void {
    if (!this.puedeConfirmarComite) return;

    this.confirmandoComite = true;

    // Si cambiaron el ganador, adjuntar la justificación al acta
    const actaFinal = this.ganadorCambiado
      ? `${this.actaComite.trim()}\n\n[JUSTIFICACIÓN CAMBIO DE GANADOR]: ${this.justificacionCambioGanador.trim()}`
      : this.actaComite.trim();

    this.http.post(`${this.API_COMITE}/solicitud/${this.idConvocatoria}/confirmar`, {
      idProcesoGanador: this.idProcesoGanador,
      actaComite:       actaFinal
    }).subscribe({
      next: () => {
        this.confirmandoComite = false;
        this.comiteConfirmado  = true;
        this.cargarComite();
        this.cargarTodo();
      },
      error: (err) => {
        this.toast.error('Error al confirmar', err?.error?.mensaje || 'No se pudo confirmar la decisión.');
        this.confirmandoComite = false;
      }
    });
  }

  // ── Override ─────────────────────────────────────────────────
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
        this.toast.error('Error al habilitar', err?.error?.mensaje || 'No se pudo habilitar al candidato.');
        this.guardandoOverride = false;
      }
    });
  }

  // ── Modal evaluadores ─────────────────────────────────────────
  abrirModalEvaluadores(c: Candidato): void {
    this.candidatoEvaluadores    = c;
    this.modalEvaluadoresVisible = true;
  }

  cerrarModalEvaluadores(): void {
    this.modalEvaluadoresVisible = false;
    this.candidatoEvaluadores    = null;
  }

  get contextLabelEvaluadores(): string {
    if (!this.candidatoEvaluadores) return '';
    return `${this.candidatoEvaluadores.apellidos} ${this.candidatoEvaluadores.nombres} — Matriz de Méritos`;
  }

  // ── Helpers ───────────────────────────────────────────────────
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
  notificarDecision(): void {
    if (this.notificando || this.notificado) return;
    this.notificando = true;

    this.http.post(`${this.API_REPORTE}/solicitud/${this.idConvocatoria}/notificar`, {})
      .subscribe({
        next: () => {
          this.notificando = false;
          this.notificado  = true;
          this.toast.success('Notificaciones enviadas',
            'Se notificó al revisor y a los postulantes. Los correos se enviarán en breve.');
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.toast.error('Error', err?.error?.mensaje || 'No se pudieron enviar las notificaciones.');
          this.notificando = false;
        }
      });
  }

  descargarActa(): void {
    if (this.descargandoActa) return;
    this.descargandoActa = true;

    this.http.get(`${this.API_REPORTE}/solicitud/${this.idConvocatoria}/acta-pdf`,
      { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = `acta-meritos-${this.idConvocatoria}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.descargandoActa = false;
      },
      error: () => {
        this.toast.error('Error', 'No se pudo generar el Acta PDF.');
        this.descargandoActa = false;
      }
    });
  }

  descargarInformeFinal(): void {
    if (this.descargandoInforme) return;
    this.descargandoInforme = true;
    this.cdr.detectChanges();

    this.http.get(`${this.API_REPORTE}/solicitud/${this.idConvocatoria}/informe-pdf`,
      { responseType: 'blob', observe: 'response' }).subscribe({
      next: (response) => {
        try {
          const blob = new Blob([response.body!], { type: 'application/pdf' });
          const url  = window.URL.createObjectURL(blob);
          const a    = document.createElement('a');
          a.href     = url;
          a.download = `informe-seleccion-${this.idConvocatoria}.pdf`;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
          this.toast.success('Informe generado', 'El informe fue descargado y enviado a la autoridad académica.');
        } finally {
          this.descargandoInforme = false;
          this.cdr.detectChanges();
        }
      },
      error: () => {
        this.toast.error('Error', 'No se pudo generar el Informe Final PDF.');
        this.descargandoInforme = false;
        this.cdr.detectChanges();
      }
    });
  }

}
