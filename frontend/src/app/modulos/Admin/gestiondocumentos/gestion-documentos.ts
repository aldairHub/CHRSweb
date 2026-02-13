import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PrepostulacionService, Prepostulacion } from '../../../services/prepostulacion.service';
import { NavbarComponent } from '../../../component/navbar';

@Component({
  selector: 'app-gestion-documentos',
  standalone: true,
  templateUrl: './gestion-documentos.html',
  styleUrls: ['./gestion-documentos.scss'],
  imports: [CommonModule, FormsModule, DatePipe, NavbarComponent]
})
export class GestionDocumentosComponent implements OnInit {
  documentos: any[] = [];
  documentosFiltrados: any[] = [];
  documentosPaginados: any[] = [];

  selectedDocumento: any = null;

  showDocumentosModal = false;
  showRechazarModal = false;
  motivoRechazo = '';

  // Estadísticas
  totalDocumentos = 0;
  documentosPendientes = 0;
  documentosValidados = 0;
  documentosRechazados = 0;

  // Filtros
  searchTerm = '';
  filterEstado = '';
  filterPostulacion = '';
  postulaciones: any[] = [];

  // Paginación
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;

  // Helper
  Math = Math;

  // Estado de carga
  cargando = true;

  constructor(
    private prepostulacionService: PrepostulacionService,
    private cdr: ChangeDetectorRef // ⭐ AGREGADO
  ) {}

  ngOnInit(): void {
    this.cargarDesdeBackend();
  }

  // ===============================
  // CARGA PRINCIPAL
  // ===============================
  cargarDesdeBackend(): void {
    this.cargando = true;
    console.log('🔄 Iniciando carga de datos...');

    this.prepostulacionService.listarPrepostulaciones().subscribe({
      next: (data: Prepostulacion[]) => {
        console.log('📥 Datos recibidos del backend:', data);

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

        console.log('✅ Documentos mapeados:', this.documentos);

        this.applyFilters();
        this.calcularEstadisticas();
        this.cargando = false;

        // ⭐ Forzar detección de cambios
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('❌ Error al cargar prepostulaciones:', err);
        alert('Error al cargar los datos. Verifica que el backend esté corriendo.');
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

  // ===============================
  // VER DOCUMENTOS
  // ===============================
  verDocumentos(doc: any): void {
    console.log('👁️ Cargando documentos para ID:', doc.id);

    this.prepostulacionService.obtenerDocumentos(doc.id).subscribe({
      next: (res) => {
        console.log('📄 URLs de documentos recibidas:', res);

        this.selectedDocumento = {
          ...doc,
          documentos: [
            {
              tipo: 'Cédula',
              nombre: 'cedula.pdf',
              formato: 'PDF',
              tamanio: '—',
              url: res.cedula
            },
            {
              tipo: 'Foto',
              nombre: 'foto.jpg',
              formato: 'IMG',
              tamanio: '—',
              url: res.foto
            },
            {
              tipo: 'Prerrequisitos',
              nombre: 'prerrequisitos.pdf',
              formato: 'PDF',
              tamanio: '—',
              url: res.prerrequisitos
            }
          ]
        };

        this.showDocumentosModal = true;
        this.cdr.detectChanges(); // ⭐ AGREGADO
      },
      error: (err: any) => {
        console.error('❌ Error al cargar documentos:', err);
        alert('Error al cargar los documentos');
      }
    });
  }

  closeDocumentosModal(): void {
    this.showDocumentosModal = false;
    this.selectedDocumento = null;
    this.cdr.detectChanges(); // ⭐ AGREGADO
  }

  // ===============================
  // ARCHIVOS
  // ===============================
  verDocumento(doc: any): void {
    if (!doc.url || doc.url === '') {
      alert('No hay archivo disponible para este documento.');
      return;
    }
    console.log('🔗 Abriendo URL:', doc.url);
    window.open(doc.url, '_blank');
  }

  descargarDocumento(doc: any): void {
    if (!doc.url || doc.url === '') {
      alert('No hay archivo disponible para descargar.');
      return;
    }
    const a = document.createElement('a');
    a.href = doc.url;
    a.download = doc.nombre || 'documento';
    a.click();
  }

  descargarDocumentos(doc?: any): void {
    if (!doc) {
      alert('Funcionalidad de descarga masiva pendiente');
      return;
    }

    this.prepostulacionService.obtenerDocumentos(doc.id).subscribe({
      next: (docs) => {
        if (docs.cedula) window.open(docs.cedula, '_blank');
        if (docs.foto) window.open(docs.foto, '_blank');
        if (docs.prerrequisitos) window.open(docs.prerrequisitos, '_blank');

        alert('✅ Abriendo todos los documentos...');
      },
      error: (err: any) => {
        console.error('❌ Error al descargar documentos:', err);
        alert('❌ Error al descargar los documentos');
      }
    });
  }

  // ===============================
  // VALIDAR / RECHAZAR
  // ===============================
  validarDocumentos(doc: any): void {
    const request = {
      estado: 'APROBADO',
      observaciones: 'Documentos validados correctamente',
      idRevisor: 1
    };

    console.log('📤 Enviando petición de validación:', request);

    this.prepostulacionService
      .actualizarEstado(doc.id, request)
      .subscribe({
        next: (response) => {
          console.log('✅ Respuesta del servidor:', response);
          doc.estado = 'validado';
          this.calcularEstadisticas();
          this.applyFilters();
          this.cdr.detectChanges(); // ⭐ AGREGADO
          alert('✅ Documentos validados correctamente');
        },
        error: (err: any) => {
          console.error('❌ Error al validar:', err);
          console.error('📋 Detalles completos del error:', {
            status: err.status,
            statusText: err.statusText,
            error: err.error,
            message: err.message
          });
          alert(`❌ Error al validar los documentos. Código: ${err.status}`);
        }
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
    this.cdr.detectChanges(); // ⭐ AGREGADO
  }

  rechazarTodosDocumentos(): void {
    if (!this.selectedDocumento) return;
    this.motivoRechazo = '';
    this.showRechazarModal = true;
    this.cdr.detectChanges(); // ⭐ AGREGADO
  }

  confirmarRechazo(): void {
    if (!this.selectedDocumento || !this.motivoRechazo.trim()) {
      alert('⚠️ Por favor ingresa un motivo de rechazo');
      return;
    }

    const request = {
      estado: 'RECHAZADO',
      observaciones: this.motivoRechazo,
      idRevisor: 1
    };

    this.prepostulacionService
      .actualizarEstado(this.selectedDocumento.id, request)
      .subscribe({
        next: (response) => {
          console.log('✅ Respuesta del servidor:', response);

          const docEnLista = this.documentos.find(d => d.id === this.selectedDocumento.id);
          if (docEnLista) {
            docEnLista.estado = 'rechazado';
          }

          this.closeRechazarModal();
          this.closeDocumentosModal();
          this.calcularEstadisticas();
          this.applyFilters();
          this.cdr.detectChanges(); // ⭐ AGREGADO
          alert('✅ Documentos rechazados correctamente');
        },
        error: (err: any) => {
          console.error('❌ Error al rechazar:', err);
          alert(`❌ Error al rechazar los documentos. Código: ${err.status}`);
        }
      });
  }

  closeRechazarModal(): void {
    this.showRechazarModal = false;
    this.motivoRechazo = '';
    this.cdr.detectChanges(); // ⭐ AGREGADO
  }

  // ===============================
  // FILTROS + PAGINACIÓN
  // ===============================
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
    this.cdr.detectChanges(); // ⭐ AGREGADO
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.documentosFiltrados.length / this.pageSize);
    const start = (this.currentPage - 1) * this.pageSize;
    this.documentosPaginados = this.documentosFiltrados.slice(start, start + this.pageSize);
    this.cdr.detectChanges(); // ⭐ AGREGADO
  }

  changePage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePagination();
  }

  getPageNumbers(): number[] {
    const maxVisible = 5;
    const pages: number[] = [];

    if (this.totalPages <= maxVisible) {
      return Array.from({ length: this.totalPages }, (_, i) => i + 1);
    }

    pages.push(1);

    if (this.currentPage > 3) {
      pages.push(-1);
    }

    for (let i = Math.max(2, this.currentPage - 1); i <= Math.min(this.totalPages - 1, this.currentPage + 1); i++) {
      pages.push(i);
    }

    if (this.currentPage < this.totalPages - 2) {
      pages.push(-1);
    }

    if (this.totalPages > 1) {
      pages.push(this.totalPages);
    }

    return pages;
  }

  // ===============================
  // ESTADÍSTICAS
  // ===============================
  calcularEstadisticas(): void {
    this.totalDocumentos = this.documentos.length;
    this.documentosPendientes = this.documentos.filter(d => d.estado === 'pendiente').length;
    this.documentosValidados = this.documentos.filter(d => d.estado === 'validado').length;
    this.documentosRechazados = this.documentos.filter(d => d.estado === 'rechazado').length;
    this.cdr.detectChanges(); // ⭐ AGREGADO
  }

  // ===============================
  // UI HELPERS
  // ===============================
  getEstadoBadgeClass(estado: string): string {
    return {
      pendiente: 'badge-warning',
      validado: 'badge-success',
      rechazado: 'badge-danger'
    }[estado] || 'badge-secondary';
  }

  getEstadoLabel(estado: string): string {
    return estado.charAt(0).toUpperCase() + estado.slice(1);
  }

  getPostulacionClass(postulacion?: string): string {
    return 'badge-info';
  }

  exportarReporte(): void {
    alert('Funcionalidad de exportación pendiente');
  }
}
