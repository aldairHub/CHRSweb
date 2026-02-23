import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PrepostulacionService, Prepostulacion } from '../../../services/prepostulacion.service';
import { NavbarComponent } from '../../../component/navbar';
import { TipoDocumentoService, TipoDocumento } from '../../../services/TipoDocumento.service';

@Component({
  selector: 'app-gestion-documentos',
  standalone: true,
  templateUrl: './gestion-documentos.html',
  styleUrls: ['./gestion-documentos.scss'],
  imports: [CommonModule, FormsModule, DatePipe, NavbarComponent]
})
export class GestionDocumentosComponent implements OnInit {

  // ── Tab activo ──────────────────────────────────────────
  tabActivo: 'postulantes' | 'tipos' = 'postulantes';

  // ── Postulantes (código original) ───────────────────────
  documentos: any[] = [];
  documentosFiltrados: any[] = [];
  documentosPaginados: any[] = [];
  selectedDocumento: any = null;
  showDocumentosModal = false;
  showRechazarModal = false;
  motivoRechazo = '';
  totalDocumentos = 0;
  documentosPendientes = 0;
  documentosValidados = 0;
  documentosRechazados = 0;
  searchTerm = '';
  filterEstado = '';
  filterPostulacion = '';
  postulaciones: any[] = [];
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  Math = Math;
  cargando = true;

  // ── Tipos de Documento ───────────────────────────────────
  tiposDocumento: TipoDocumento[] = [];
  showTipoModal = false;
  tipoEditando: TipoDocumento | null = null;
  tipoForm = { nombre: '', descripcion: '', obligatorio: false };

  constructor(
    private prepostulacionService: PrepostulacionService,
    private tipoDocumentoService: TipoDocumentoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarDesdeBackend();
  }

  // ===============================
  // TAB: TIPOS DE DOCUMENTO
  // ===============================
  cargarTipos(): void {
    this.tipoDocumentoService.listar().subscribe({
      next: data => {
        this.tiposDocumento = data;
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('Error al cargar tipos:', err);
        this.mostrarToast('error', 'Error al cargar tipos de documento');
      }
    });
  }

  abrirModalCrear(): void {
    this.tipoEditando = null;
    this.tipoForm = { nombre: '', descripcion: '', obligatorio: false };
    this.showTipoModal = true;
    this.cdr.detectChanges();
  }

  abrirModalEditar(tipo: TipoDocumento): void {
    this.tipoEditando = tipo;
    this.tipoForm = {
      nombre: tipo.nombre,
      descripcion: tipo.descripcion || '',
      obligatorio: tipo.obligatorio
    };
    this.showTipoModal = true;
    this.cdr.detectChanges();
  }

  closeTipoModal(): void {
    this.showTipoModal = false;
    this.tipoEditando = null;
    this.cdr.detectChanges();
  }

  guardarTipo(): void {
    if (!this.tipoForm.nombre.trim()) return;

    if (this.tipoEditando) {
      this.tipoDocumentoService.editar(this.tipoEditando.idTipoDocumento, this.tipoForm).subscribe({
        next: res => {
          if (res.exitoso) {
            this.mostrarToast('success', 'Tipo actualizado correctamente');
            this.closeTipoModal();
            this.cargarTipos();
          } else {
            this.mostrarToast('error', res.mensaje || 'Error al actualizar');
          }
        },
        error: () => this.mostrarToast('error', 'Error al actualizar el tipo')
      });
    } else {
      this.tipoDocumentoService.crear(this.tipoForm).subscribe({
        next: res => {
          const msg = res.mensaje || '';
          if (msg.startsWith('ERROR')) {
            this.mostrarToast('error', msg.replace('ERROR: ', ''));
          } else {
            this.mostrarToast('success', 'Tipo creado correctamente');
            this.closeTipoModal();
            this.cargarTipos();
          }
        },
        error: () => this.mostrarToast('error', 'Error al crear el tipo')
      });
    }
  }

  toggleTipo(tipo: TipoDocumento): void {
    this.tipoDocumentoService.toggle(tipo.idTipoDocumento).subscribe({
      next: res => {
        if (res.exitoso) {
          tipo.activo = res.activo;
          this.mostrarToast('success', `Tipo ${res.activo ? 'activado' : 'desactivado'}`);
          this.cdr.detectChanges();
        }
      },
      error: () => this.mostrarToast('error', 'Error al cambiar estado')
    });
  }

  // ── Toast simple ─────────────────────────────────────────
  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  mostrarToast(tipo: 'success' | 'error', mensaje: string): void {
    this.toastType = tipo;
    this.toastMessage = mensaje;
    this.showToast = true;
    clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => {
      this.showToast = false;
      this.cdr.detectChanges();
    }, 2500);
  }

  // ===============================
  // CÓDIGO ORIGINAL - POSTULANTES
  // ===============================
  cargarDesdeBackend(): void {
    this.cargando = true;
    this.prepostulacionService.listarPrepostulaciones().subscribe({
      next: (data: Prepostulacion[]) => {
        this.documentos = data.map(p => ({
          id: p.idPrepostulacion,
          nombreCompleto: `${p.nombres} ${p.apellidos}`,
          cedula: p.identificacion,
          correo: p.correo,
          postulacion: 'N/A',
          estado: this.mapEstado(p.estadoRevision),
          fechaEnvio: new Date(p.fechaEnvio),
          documentos: [
            { tipo: 'Cédula', nombre: 'Cédula' },
            { tipo: 'Foto', nombre: 'Foto' },
            { tipo: 'Prerrequisitos', nombre: 'Prerrequisitos' }
          ]
        }));
        this.applyFilters();
        this.calcularEstadisticas();
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('Error al cargar prepostulaciones:', err);
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  mapEstado(estado: string): string {
    if (estado === 'APROBADO') return 'validado';
    if (estado === 'RECHAZADO') return 'rechazado';
    return 'pendiente';
  }

  verDocumentos(doc: any): void {
    this.prepostulacionService.obtenerDocumentos(doc.id).subscribe({
      next: (res) => {
        this.selectedDocumento = {
          ...doc,
          documentos: [
            { tipo: 'Cédula', nombre: 'cedula.pdf', formato: 'PDF', tamanio: '—', url: res.cedula },
            { tipo: 'Foto', nombre: 'foto.jpg', formato: 'IMG', tamanio: '—', url: res.foto },
            { tipo: 'Prerrequisitos', nombre: 'prerrequisitos.pdf', formato: 'PDF', tamanio: '—', url: res.prerrequisitos }
          ]
        };
        this.showDocumentosModal = true;
        this.cdr.detectChanges();
      },
      error: () => alert('Error al cargar los documentos')
    });
  }

  closeDocumentosModal(): void {
    this.showDocumentosModal = false;
    this.selectedDocumento = null;
    this.cdr.detectChanges();
  }

  verDocumento(doc: any): void {
    if (!doc.url) { alert('No hay archivo disponible.'); return; }
    window.open(doc.url, '_blank');
  }

  descargarDocumento(doc: any): void {
    if (!doc.url) { alert('No hay archivo disponible.'); return; }
    const a = document.createElement('a');
    a.href = doc.url;
    a.download = doc.nombre || 'documento';
    a.click();
  }

  descargarDocumentos(doc?: any): void {
    if (!doc) return;
    this.prepostulacionService.obtenerDocumentos(doc.id).subscribe({
      next: (docs) => {
        if (docs.cedula) window.open(docs.cedula, '_blank');
        if (docs.foto) window.open(docs.foto, '_blank');
        if (docs.prerrequisitos) window.open(docs.prerrequisitos, '_blank');
      },
      error: () => alert('Error al descargar los documentos')
    });
  }

  validarDocumentos(doc: any): void {
    const request = { estado: 'APROBADO', observaciones: 'Documentos validados correctamente', idRevisor: 1 };
    this.prepostulacionService.actualizarEstado(doc.id, request).subscribe({
      next: () => {
        doc.estado = 'validado';
        this.calcularEstadisticas();
        this.applyFilters();
        this.cdr.detectChanges();
        this.mostrarToast('success', 'Documentos validados correctamente');
      },
      error: (err: any) => this.mostrarToast('error', `Error al validar. Código: ${err.status}`)
    });
  }

  validarTodosDocumentos(): void {
    if (!this.selectedDocumento) return;
    this.validarDocumentos(this.selectedDocumento);
    this.closeDocumentosModal();
  }

  rechazarDocumentos(doc: any): void {
    this.selectedDocumento = doc;
    this.motivoRechazo = '';
    this.showRechazarModal = true;
    this.cdr.detectChanges();
  }

  rechazarTodosDocumentos(): void {
    if (!this.selectedDocumento) return;
    this.motivoRechazo = '';
    this.showRechazarModal = true;
    this.cdr.detectChanges();
  }

  confirmarRechazo(): void {
    if (!this.selectedDocumento || !this.motivoRechazo.trim()) return;
    const request = { estado: 'RECHAZADO', observaciones: this.motivoRechazo, idRevisor: 1 };
    this.prepostulacionService.actualizarEstado(this.selectedDocumento.id, request).subscribe({
      next: () => {
        const d = this.documentos.find(d => d.id === this.selectedDocumento.id);
        if (d) d.estado = 'rechazado';
        this.closeRechazarModal();
        this.closeDocumentosModal();
        this.calcularEstadisticas();
        this.applyFilters();
        this.cdr.detectChanges();
        this.mostrarToast('success', 'Documentos rechazados correctamente');
      },
      error: (err: any) => this.mostrarToast('error', `Error al rechazar. Código: ${err.status}`)
    });
  }

  closeRechazarModal(): void {
    this.showRechazarModal = false;
    this.motivoRechazo = '';
    this.cdr.detectChanges();
  }

  applyFilters(): void {
    this.documentosFiltrados = this.documentos.filter(d => {
      const matchSearch =
        d.nombreCompleto.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        d.cedula.includes(this.searchTerm) ||
        d.correo.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchEstado = !this.filterEstado || d.estado === this.filterEstado;
      const matchPostulacion = !this.filterPostulacion || d.postulacion === this.filterPostulacion;
      return matchSearch && matchEstado && matchPostulacion;
    });
    this.currentPage = 1;
    this.updatePagination();
    this.cdr.detectChanges();
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.documentosFiltrados.length / this.pageSize);
    const start = (this.currentPage - 1) * this.pageSize;
    this.documentosPaginados = this.documentosFiltrados.slice(start, start + this.pageSize);
    this.cdr.detectChanges();
  }

  changePage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePagination();
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    if (this.totalPages <= 5) return Array.from({ length: this.totalPages }, (_, i) => i + 1);
    pages.push(1);
    if (this.currentPage > 3) pages.push(-1);
    for (let i = Math.max(2, this.currentPage - 1); i <= Math.min(this.totalPages - 1, this.currentPage + 1); i++) pages.push(i);
    if (this.currentPage < this.totalPages - 2) pages.push(-1);
    if (this.totalPages > 1) pages.push(this.totalPages);
    return pages;
  }

  calcularEstadisticas(): void {
    this.totalDocumentos = this.documentos.length;
    this.documentosPendientes = this.documentos.filter(d => d.estado === 'pendiente').length;
    this.documentosValidados = this.documentos.filter(d => d.estado === 'validado').length;
    this.documentosRechazados = this.documentos.filter(d => d.estado === 'rechazado').length;
    this.cdr.detectChanges();
  }

  getEstadoBadgeClass(estado: string): string {
    return ({ pendiente: 'badge-warning', validado: 'badge-success', rechazado: 'badge-danger' } as any)[estado] || 'badge-secondary';
  }

  getEstadoLabel(estado: string): string {
    return estado.charAt(0).toUpperCase() + estado.slice(1);
  }

  getPostulacionClass(postulacion?: string): string { return 'badge-info'; }

  exportarReporte(): void { alert('Funcionalidad de exportación pendiente'); }
}
