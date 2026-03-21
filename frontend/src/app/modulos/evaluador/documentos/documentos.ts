import { Component, OnInit,ChangeDetectorRef} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import {
  DocumentoService,
  DocumentoBackend
} from '../../../services/documento.service';
import { ToastService } from '../../../services/toast.service';
import { ToastComponent } from '../../../component/toast.component';

export interface DocumentoRevision {
  idDocumento:     number;
  idTipoDocumento: number;
  nombre:          string;
  descripcion:     string;
  obligatorio:     boolean;
  rutaArchivo:     string | null;
  nombreArchivo:   string;
  fechaCarga:      string | null;
  observacionIa:   string;
  estado:          'pendiente' | 'subido' | 'validado' | 'rechazado';
  estadoEval:      'sin_revisar' | 'validado' | 'rechazado';
  observacionEval: string;
  guardando:       boolean;
  guardado:        boolean;
  bloqueado:       boolean; // true cuando ya está validado — no se puede cambiar
}

@Component({
  selector: 'app-documentos',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastComponent],
  templateUrl: './documentos.html',
  styleUrls: ['./documentos.scss']
})
export class DocumentosComponent implements OnInit {

  idPostulacion!: number;
  idSolicitud:    number | null = null;   // para poder volver al paso 3
  postulante = { nombres: '', apellidos: '', cedula: '', estado: '' };
  documentos:    DocumentoRevision[] = [];
  cargando       = true;
  guardandoTodo  = false;
  error: string | null = null;

  constructor(
    private route:        ActivatedRoute,
    private router:       Router,
    private documentoSvc: DocumentoService,private cdr: ChangeDetectorRef,
    private toast:        ToastService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = Number(params['id']);
      if (!id || isNaN(id)) {
        this.router.navigate(['/evaluador/postulantes']);
        this.cdr.detectChanges();
        return;
      }
      this.idPostulacion = id;
      // Leer idSolicitud del queryParam para poder volver al paso 3
      this.route.queryParams.subscribe(qp => {
        this.idSolicitud = qp['idSolicitud'] ? Number(qp['idSolicitud']) : null;
      });
      this.cargarTodo();
      this.cdr.detectChanges();
    });
  }

  cargarTodo(): void {
    this.cargando = true;
    this.error    = null;

    this.documentoSvc.obtenerInfoPorPostulacion(this.idPostulacion).subscribe({
      next: (info: any) => {
        this.postulante.nombres   = info['nombres']            ?? '';
        this.postulante.apellidos = info['apellidos']          ?? '';
        this.postulante.cedula    = info['identificacion']     ?? '';
        this.postulante.estado    = info['estado_postulacion'] ?? '';
        this.cdr.detectChanges();
      },
      error: () => {}
    });

    this.documentoSvc.obtenerDocumentos(this.idPostulacion).subscribe({
      next: (docs: DocumentoBackend[]) => {
        this.documentos = docs
          .filter(d => d.idDocumento !== null)
          .map(d => this.mapear(d));
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error    = 'No se pudo cargar la informacion de documentos.';
        this.cargando = false;
      }
    });
  }

  private mapear(d: DocumentoBackend): DocumentoRevision {
    const estadoRaw = (d.estadoValidacion ?? '').toLowerCase().trim();
    let estado: DocumentoRevision['estado'] = 'pendiente';
    if      (estadoRaw === 'validado')  estado = 'validado';
    else if (estadoRaw === 'rechazado') estado = 'rechazado';
    else if (d.idDocumento)             estado = 'subido';
    this.cdr.detectChanges();
    let estadoEval: DocumentoRevision['estadoEval'] = 'sin_revisar';
    if (estado === 'validado')  estadoEval = 'validado';
    if (estado === 'rechazado') estadoEval = 'rechazado';
    this.cdr.detectChanges();
    const nombreArchivo = d.rutaArchivo
      ? d.rutaArchivo.replace(/\\/g, '/').split('/').pop() ?? 'documento.pdf'
      : '';

    // Ya guardado si tiene estado definitivo
    const guardado = estado === 'validado' || estado === 'rechazado';
    // Bloqueado si ya fue VALIDADO — no se puede cambiar a rechazado
    const bloqueado = estado === 'validado';

    return {
      idDocumento:     d.idDocumento!,
      idTipoDocumento: d.idTipoDocumento,
      nombre:          d.nombreTipo,
      descripcion:     d.descripcionTipo ?? 'Documento requerido para el proceso.',
      obligatorio:     d.obligatorio,
      rutaArchivo:     d.rutaArchivo,
      nombreArchivo,
      fechaCarga:      d.fechaCarga,
      observacionIa:   d.observacionesIa ?? '',
      estado,
      estadoEval,
      // ← CLAVE: observacionEval empieza vacía, NO con la observación de la IA
      observacionEval: '',
      guardando:       false,
      guardado,
      bloqueado
    };
  }

  get nombreCompleto(): string {
    return (this.postulante.nombres + ' ' + this.postulante.apellidos).trim();
  }

  get totalRevisados(): number {
    return this.documentos.filter(d => d.estadoEval !== 'sin_revisar').length;
  }

  get porcentajeRevisado(): number {
    if (!this.documentos.length) return 0;
    return Math.round((this.totalRevisados / this.documentos.length) * 100);
  }

  get todosRevisados(): boolean {
    return this.documentos.length > 0 &&
           this.documentos.every(d => d.estadoEval !== 'sin_revisar');
  }

  verArchivo(url: string | null): void {
    if (!url) {
      this.toast.error('Sin archivo', 'No hay archivo disponible.');
      return;
    }
    window.open(url, '_blank');
  }

  seleccionarEstado(doc: DocumentoRevision, nuevoEstado: 'validado' | 'rechazado'): void {
    // Si ya está validado y guardado → no permitir cambio
    if (doc.bloqueado) {
      this.toast.error('No permitido', 'Este documento ya fue validado y no puede rechazarse.');
      return;
    }
    doc.estadoEval = nuevoEstado;
    doc.guardado   = false;
  }

  guardarDoc(doc: DocumentoRevision): void {
    if (doc.bloqueado) return;
    if (doc.estadoEval === 'sin_revisar') {
      this.toast.error('Sin decision', 'Selecciona Valido o Rechazado primero.');
      return;
    }
    if (doc.estadoEval === 'rechazado' && !doc.observacionEval.trim()) {
      this.toast.error('Observacion requerida', 'Agrega una observacion al rechazar.');
      return;
    }
    doc.guardando = true;
    this.documentoSvc.validarDocumento(
      doc.idDocumento,
      doc.estadoEval as 'validado' | 'rechazado',
      doc.observacionEval
    ).subscribe({
      next: () => {
        doc.estado    = doc.estadoEval as 'validado' | 'rechazado';
        doc.guardado  = true;
        doc.guardando = false;
        // Si se validó → bloquear para no poder rechazar después
        if (doc.estadoEval === 'validado') doc.bloqueado = true;
        this.toast.success('Guardado', doc.nombre + ' marcado como ' + doc.estadoEval + '.');
      },
      error: () => {
        doc.guardando = false;
        this.toast.error('Error', 'No se pudo guardar. Intenta nuevamente.');
      }
    });
  }

  guardarTodos(): void {
    const sinRevisar = this.documentos.filter(d => d.estadoEval === 'sin_revisar');
    if (sinRevisar.length) {
      this.toast.error('Incompleto', 'Quedan ' + sinRevisar.length + ' documentos sin revisar.');
      return;
    }
    const sinObs = this.documentos.filter(
      d => d.estadoEval === 'rechazado' && !d.observacionEval.trim()
    );
    if (sinObs.length) {
      this.toast.error('Observacion requerida', 'Agrega observacion a los documentos rechazados.');
      return;
    }

    this.guardandoTodo = true;
    const pendientes   = this.documentos.filter(d => !d.guardado && !d.bloqueado);

    if (!pendientes.length) {
      this.guardandoTodo = false;
      this.toast.success('Todo listo', 'Todos los documentos ya estaban guardados.');
      return;
    }

    let completados = 0;
    pendientes.forEach(doc => {
      this.documentoSvc.validarDocumento(
        doc.idDocumento,
        doc.estadoEval as 'validado' | 'rechazado',
        doc.observacionEval
      ).subscribe({
        next: () => {
          doc.estado   = doc.estadoEval as 'validado' | 'rechazado';
          doc.guardado = true;
          if (doc.estadoEval === 'validado') doc.bloqueado = true;
          completados++;
          if (completados === pendientes.length) {
            this.guardandoTodo = false;
            this.toast.success('Revision completada', 'Todos los documentos fueron guardados.');
            // Actualizar estado de postulación si TODOS los docs están validados
            this.actualizarEstadoPostulacionSiCorresponde();
          }
        },
        error: () => {
          completados++;
          if (completados === pendientes.length) this.guardandoTodo = false;
          this.toast.error('Error parcial', 'No se pudo guardar ' + doc.nombre + '.');
        }
      });
    });
  }

  volver(): void {
    // Si tenemos idSolicitud volvemos directo al paso 3 (lista de postulantes)
    if (this.idSolicitud) {
      this.router.navigate(['/evaluador/postulantes'],
        { queryParams: { idSolicitud: this.idSolicitud } });
    } else {
      this.router.navigate(['/evaluador/postulantes']);
    }
  }

  /** true cuando todos los docs tienen estado guardado (validado o rechazado) */
  get todosGuardados(): boolean {
    return this.documentos.length > 0 &&
           this.documentos.every(d => d.guardado || d.bloqueado);
  }

  /** Cambia el estado de la postulación a "aprobada" si todos los docs están validados */
  private actualizarEstadoPostulacionSiCorresponde(): void {
    const todosValidados = this.documentos.every(d => d.estado === 'validado');
    if (todosValidados) {
      this.documentoSvc.actualizarEstadoPostulacion(this.idPostulacion, 'aprobada')
        .subscribe({
          next: () => {
            this.postulante.estado = 'aprobada';
            this.toast.success(
              '¡Postulación aprobada!',
              'Todos los documentos están validados. La postulación pasó a estado Aprobada.'
            );
            this.cdr.detectChanges();
          },
          error: () => {
            // No crítico — los documentos ya se guardaron
          }
        });
    }
  }
}
