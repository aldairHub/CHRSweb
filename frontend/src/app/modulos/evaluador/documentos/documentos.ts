import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import {
  DocumentoService,
  DocumentoBackend
} from '../../../services/documento.service';
import { ToastService } from '../../../services/toast.service';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';
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
}

@Component({
  selector: 'app-documentos',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, FooterComponent, ToastComponent],
  templateUrl: './documentos.html',
  styleUrls: ['./documentos.scss']
})
export class DocumentosComponent implements OnInit {

  idPostulacion!: number;

  postulante = { nombres: '', apellidos: '', cedula: '', estado: '' };

  documentos:    DocumentoRevision[] = [];
  cargando       = true;
  guardandoTodo  = false;
  error: string | null = null;

  constructor(
    private route:        ActivatedRoute,
    private router:       Router,
    private documentoSvc: DocumentoService,
    private toast:        ToastService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.idPostulacion = +params['id'];
      this.cargarTodo();
    });
  }

  cargarTodo(): void {
    this.cargando = true;
    this.error    = null;

    this.documentoSvc.obtenerInfoPorPostulacion(this.idPostulacion).subscribe({
      next: (info: any) => {
        this.postulante.nombres   = info['nombres']         ?? '';
        this.postulante.apellidos = info['apellidos']       ?? '';
        this.postulante.cedula    = info['identificacion']  ?? '';
        this.postulante.estado    = info['estado_postulacion'] ?? '';
      },
      error: () => {}
    });

    this.documentoSvc.obtenerDocumentos(this.idPostulacion).subscribe({
      next: (docs: DocumentoBackend[]) => {
        this.documentos = docs
          .filter(d => d.idDocumento !== null)
          .map(d => this.mapear(d));
        this.cargando = false;
      },
      error: () => {
        this.error    = 'No se pudo cargar la información de documentos.';
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

    let estadoEval: DocumentoRevision['estadoEval'] = 'sin_revisar';
    if (estado === 'validado')  estadoEval = 'validado';
    if (estado === 'rechazado') estadoEval = 'rechazado';

    const nombreArchivo = d.rutaArchivo
      ? d.rutaArchivo.replace(/\\/g, '/').split('/').pop() ?? 'documento.pdf'
      : '';

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
      observacionEval: d.observacionesIa ?? '',
      guardando:       false,
      guardado:        estado === 'validado' || estado === 'rechazado'
    };
  }

  get nombreCompleto(): string {
    return `${this.postulante.nombres} ${this.postulante.apellidos}`.trim();
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

  guardarDoc(doc: DocumentoRevision): void {
    if (doc.estadoEval === 'sin_revisar') {
      this.toast.error('Sin decisión', 'Selecciona Válido o Rechazado primero.');
      return;
    }
    if (doc.estadoEval === 'rechazado' && !doc.observacionEval.trim()) {
      this.toast.error('Observación requerida', 'Agrega una observación al rechazar.');
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
        this.toast.success('Guardado', `"${doc.nombre}" marcado como ${doc.estadoEval}.`);
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
      this.toast.error('Incompleto', `Quedan ${sinRevisar.length} documentos sin revisar.`);
      return;
    }
    const sinObs = this.documentos.filter(
      d => d.estadoEval === 'rechazado' && !d.observacionEval.trim()
    );
    if (sinObs.length) {
      this.toast.error('Observación requerida', 'Agrega observación a los documentos rechazados.');
      return;
    }

    this.guardandoTodo = true;
    const pendientes   = this.documentos.filter(d => !d.guardado);

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
          completados++;
          if (completados === pendientes.length) {
            this.guardandoTodo = false;
            this.toast.success('¡Revisión completada!', 'Todos los documentos fueron guardados.');
          }
        },
        error: () => {
          completados++;
          if (completados === pendientes.length) this.guardandoTodo = false;
          this.toast.error('Error parcial', `No se pudo guardar "${doc.nombre}".`);
        }
      });
    });
  }

  volver(): void {
    this.router.navigate(['/evaluador/postulantes']);
  }
}
