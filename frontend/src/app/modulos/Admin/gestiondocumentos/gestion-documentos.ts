// gestion-documentos.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar';

interface Documento {
  tipo: string;
  nombre: string;
  formato: string;
  tamanio: string;
  url?: string;
}

interface PostulanteDocumento {
  id: number;
  nombreCompleto: string;
  cedula: string;
  correo: string;
  postulacion: string;
  documentos: Documento[];
  estado: 'validado' | 'pendiente' | 'rechazado';
  fechaEnvio: Date;
  motivoRechazo?: string;
}

interface Postulacion {
  id: string;
  nombre: string;
}

@Component({
  selector: 'app-gestion-documentos',
  standalone: true,
  imports: [
    CommonModule,   // ngIf, ngFor, ngClass
    FormsModule,    // ngModel
    DatePipe,       // | date
    NavbarComponent // <app-navbar>
  ],
  templateUrl: 'gestion-documentos.html',
  styleUrls: ['gestion-documentos.scss']
})

export class GestionDocumentosComponent implements OnInit {
  // Datos
  documentos: PostulanteDocumento[] = [];
  documentosFiltrados: PostulanteDocumento[] = [];
  documentosPaginados: PostulanteDocumento[] = [];

  // Postulaciones disponibles
  postulaciones: Postulacion[] = [
    { id: 'docente', nombre: 'Docente Tiempo Completo' },
    { id: 'coordinador', nombre: 'Coordinador de Carrera' },
    { id: 'director', nombre: 'Director de Departamento' }
  ];

  // Filtros
  searchTerm: string = '';
  filterPostulacion: string = '';
  filterEstado: string = '';

  // Paginación
  currentPage: number = 1;
  pageSize: number = 10;
  totalPages: number = 1;

  // Modales
  showDocumentosModal: boolean = false;
  showRechazarModal: boolean = false;
  selectedDocumento: PostulanteDocumento | null = null;
  motivoRechazo: string = '';

  // Estadísticas
  totalDocumentos: number = 0;
  documentosValidados: number = 0;
  documentosPendientes: number = 0;
  documentosRechazados: number = 0;

  // Utilitario
  Math = Math;

  ngOnInit(): void {
    this.cargarDatosMock();
    this.calcularEstadisticas();
    this.applyFilters();
  }

  cargarDatosMock(): void {
    this.documentos = [
      {
        id: 1,
        nombreCompleto: 'María José Rodríguez Loor',
        cedula: '1234567890',
        correo: 'maria.rodriguez@gmail.com',
        postulacion: 'Docente Tiempo Completo',
        estado: 'validado',
        fechaEnvio: new Date('2024-01-15T10:30:00'),
        documentos: [
          { tipo: 'Cédula', nombre: 'cedula_maria_rodriguez.pdf', formato: 'PDF', tamanio: '1.2 MB' },
          { tipo: 'Título', nombre: 'titulo_tercer_nivel.pdf', formato: 'PDF', tamanio: '2.5 MB' },
          { tipo: 'CV', nombre: 'curriculum_vitae.pdf', formato: 'PDF', tamanio: '850 KB' },
          { tipo: 'Certificados', nombre: 'certificados_capacitacion.pdf', formato: 'PDF', tamanio: '3.1 MB' }
        ]
      },
      {
        id: 2,
        nombreCompleto: 'Carlos Andrés Mendoza Vera',
        cedula: '0987654321',
        correo: 'carlos.mendoza@hotmail.com',
        postulacion: 'Coordinador de Carrera',
        estado: 'pendiente',
        fechaEnvio: new Date('2024-01-20T14:15:00'),
        documentos: [
          { tipo: 'Cédula', nombre: 'cedula_carlos.pdf', formato: 'PDF', tamanio: '1.1 MB' },
          { tipo: 'Título', nombre: 'titulo_maestria.pdf', formato: 'PDF', tamanio: '2.8 MB' },
          { tipo: 'CV', nombre: 'hoja_vida.pdf', formato: 'PDF', tamanio: '1.5 MB' },
          { tipo: 'Exp. Laboral', nombre: 'certificados_trabajo.pdf', formato: 'PDF', tamanio: '2.2 MB' },
          { tipo: 'Publicaciones', nombre: 'articulos_cientificos.pdf', formato: 'PDF', tamanio: '4.5 MB' }
        ]
      },
      {
        id: 3,
        nombreCompleto: 'Ana Gabriela Salazar Morán',
        cedula: '1122334455',
        correo: 'ana.salazar@uteq.edu.ec',
        postulacion: 'Director de Departamento',
        estado: 'pendiente',
        fechaEnvio: new Date('2024-01-22T09:45:00'),
        documentos: [
          { tipo: 'Cédula', nombre: 'identificacion.pdf', formato: 'PDF', tamanio: '980 KB' },
          { tipo: 'Título', nombre: 'phd_certificado.pdf', formato: 'PDF', tamanio: '3.2 MB' },
          { tipo: 'CV', nombre: 'cv_completo.pdf', formato: 'PDF', tamanio: '1.8 MB' },
          { tipo: 'Certificados', nombre: 'cursos_postgrado.pdf', formato: 'PDF', tamanio: '2.5 MB' }
        ]
      },
      {
        id: 4,
        nombreCompleto: 'Jorge Luis Palacios Castro',
        cedula: '5566778899',
        correo: 'jorge.palacios@yahoo.com',
        postulacion: 'Docente Tiempo Completo',
        estado: 'rechazado',
        fechaEnvio: new Date('2024-01-18T16:20:00'),
        motivoRechazo: 'Documentos incompletos. Falta certificado de experiencia laboral y título debidamente registrado en SENESCYT.',
        documentos: [
          { tipo: 'Cédula', nombre: 'cedula.pdf', formato: 'PDF', tamanio: '1.0 MB' },
          { tipo: 'CV', nombre: 'cv.pdf', formato: 'PDF', tamanio: '750 KB' }
        ]
      },
      {
        id: 5,
        nombreCompleto: 'Sofía Elizabeth Torres Zambrano',
        cedula: '9988776655',
        correo: 'sofia.torres@gmail.com',
        postulacion: 'Coordinador de Carrera',
        estado: 'validado',
        fechaEnvio: new Date('2024-01-25T11:30:00'),
        documentos: [
          { tipo: 'Cédula', nombre: 'cedula_sofia.pdf', formato: 'PDF', tamanio: '1.3 MB' },
          { tipo: 'Título', nombre: 'titulo_maestria_educacion.pdf', formato: 'PDF', tamanio: '2.9 MB' },
          { tipo: 'CV', nombre: 'curriculum.pdf', formato: 'PDF', tamanio: '1.6 MB' },
          { tipo: 'Exp. Laboral', nombre: 'experiencia_docente.pdf', formato: 'PDF', tamanio: '2.0 MB' },
          { tipo: 'Certificados', nombre: 'certificaciones.pdf', formato: 'PDF', tamanio: '3.5 MB' }
        ]
      },
      {
        id: 6,
        nombreCompleto: 'Roberto Carlos Intriago Flores',
        cedula: '4433221100',
        correo: 'roberto.intriago@outlook.com',
        postulacion: 'Director de Departamento',
        estado: 'pendiente',
        fechaEnvio: new Date('2024-01-26T13:00:00'),
        documentos: [
          { tipo: 'Cédula', nombre: 'cedula_roberto.pdf', formato: 'PDF', tamanio: '1.1 MB' },
          { tipo: 'Título', nombre: 'doctorado_administracion.pdf', formato: 'PDF', tamanio: '3.8 MB' },
          { tipo: 'CV', nombre: 'hoja_de_vida.pdf', formato: 'PDF', tamanio: '2.1 MB' },
          { tipo: 'Publicaciones', nombre: 'investigaciones.pdf', formato: 'PDF', tamanio: '5.2 MB' }
        ]
      },
      {
        id: 7,
        nombreCompleto: 'Patricia Fernanda Moreira Villegas',
        cedula: '7788990011',
        correo: 'patricia.moreira@uteq.edu.ec',
        postulacion: 'Docente Tiempo Completo',
        estado: 'validado',
        fechaEnvio: new Date('2024-01-28T08:45:00'),
        documentos: [
          { tipo: 'Cédula', nombre: 'identificacion_patricia.pdf', formato: 'PDF', tamanio: '1.2 MB' },
          { tipo: 'Título', nombre: 'licenciatura.pdf', formato: 'PDF', tamanio: '2.3 MB' },
          { tipo: 'CV', nombre: 'cv_actualizado.pdf', formato: 'PDF', tamanio: '1.4 MB' },
          { tipo: 'Certificados', nombre: 'capacitaciones.pdf', formato: 'PDF', tamanio: '2.8 MB' }
        ]
      },
      {
        id: 8,
        nombreCompleto: 'Diego Alejandro Cedeño Mera',
        cedula: '2211009988',
        correo: 'diego.cedeno@gmail.com',
        postulacion: 'Coordinador de Carrera',
        estado: 'rechazado',
        fechaEnvio: new Date('2024-01-19T15:30:00'),
        motivoRechazo: 'El título presentado no corresponde al área de conocimiento requerida para el cargo.',
        documentos: [
          { tipo: 'Cédula', nombre: 'cedula_diego.pdf', formato: 'PDF', tamanio: '1.0 MB' },
          { tipo: 'Título', nombre: 'titulo.pdf', formato: 'PDF', tamanio: '2.1 MB' },
          { tipo: 'CV', nombre: 'curriculum.pdf', formato: 'PDF', tamanio: '1.2 MB' }
        ]
      }
    ];
  }

  calcularEstadisticas(): void {
    this.totalDocumentos = this.documentos.length;
    this.documentosValidados = this.documentos.filter(d => d.estado === 'validado').length;
    this.documentosPendientes = this.documentos.filter(d => d.estado === 'pendiente').length;
    this.documentosRechazados = this.documentos.filter(d => d.estado === 'rechazado').length;
  }

  applyFilters(): void {
    this.documentosFiltrados = this.documentos.filter(doc => {
      const matchesSearch = !this.searchTerm ||
        doc.nombreCompleto.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        doc.cedula.includes(this.searchTerm) ||
        doc.correo.toLowerCase().includes(this.searchTerm.toLowerCase());

      const matchesPostulacion = !this.filterPostulacion ||
        this.getPostulacionId(doc.postulacion) === this.filterPostulacion;

      const matchesEstado = !this.filterEstado || doc.estado === this.filterEstado;

      return matchesSearch && matchesPostulacion && matchesEstado;
    });

    this.totalPages = Math.ceil(this.documentosFiltrados.length / this.pageSize);
    this.currentPage = 1;
    this.updatePaginatedData();
  }

  updatePaginatedData(): void {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    this.documentosPaginados = this.documentosFiltrados.slice(startIndex, endIndex);
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePaginatedData();
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;

    if (this.totalPages <= maxVisible) {
      for (let i = 1; i <= this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      if (this.currentPage <= 3) {
        pages.push(1, 2, 3, 4, -1, this.totalPages);
      } else if (this.currentPage >= this.totalPages - 2) {
        pages.push(1, -1, this.totalPages - 3, this.totalPages - 2, this.totalPages - 1, this.totalPages);
      } else {
        pages.push(1, -1, this.currentPage - 1, this.currentPage, this.currentPage + 1, -1, this.totalPages);
      }
    }

    return pages;
  }

  // Utilidades
  getPostulacionClass(postulacion: string): string {
    if (postulacion.includes('Docente')) return 'docente';
    if (postulacion.includes('Coordinador')) return 'coordinador';
    if (postulacion.includes('Director')) return 'director';
    return '';
  }

  getPostulacionId(postulacion: string): string {
    if (postulacion.includes('Docente')) return 'docente';
    if (postulacion.includes('Coordinador')) return 'coordinador';
    if (postulacion.includes('Director')) return 'director';
    return '';
  }

  getEstadoBadgeClass(estado: string): string {
    switch (estado) {
      case 'validado': return 'success';
      case 'pendiente': return 'warning';
      case 'rechazado': return 'danger';
      default: return '';
    }
  }

  getEstadoLabel(estado: string): string {
    switch (estado) {
      case 'validado': return 'Validado';
      case 'pendiente': return 'Pendiente';
      case 'rechazado': return 'Rechazado';
      default: return estado;
    }
  }

  // Acciones
  verDocumentos(doc: PostulanteDocumento): void {
    this.selectedDocumento = doc;
    this.showDocumentosModal = true;
  }

  validarDocumentos(doc: PostulanteDocumento): void {
    if (confirm(`¿Está seguro de validar los documentos de ${doc.nombreCompleto}?`)) {
      doc.estado = 'validado';
      this.calcularEstadisticas();
      this.applyFilters();
      alert('Documentos validados correctamente');
    }
  }

  rechazarDocumentos(doc: PostulanteDocumento): void {
    this.selectedDocumento = doc;
    this.motivoRechazo = '';
    this.showRechazarModal = true;
  }

  confirmarRechazo(): void {
    if (this.selectedDocumento && this.motivoRechazo.trim()) {
      this.selectedDocumento.estado = 'rechazado';
      this.selectedDocumento.motivoRechazo = this.motivoRechazo;
      this.calcularEstadisticas();
      this.applyFilters();
      this.closeRechazarModal();
      alert('Documentos rechazados correctamente');
    }
  }

  descargarDocumentos(doc: PostulanteDocumento): void {
    alert(`Descargando todos los documentos de ${doc.nombreCompleto}...`);
    // Aquí iría la lógica real de descarga
  }

  verDocumento(documento: Documento): void {
    alert(`Abriendo documento: ${documento.nombre}`);
    // Aquí iría la lógica para abrir el documento
  }

  descargarDocumento(documento: Documento): void {
    alert(`Descargando: ${documento.nombre}`);
    // Aquí iría la lógica real de descarga
  }

  validarTodosDocumentos(): void {
    if (this.selectedDocumento && confirm(`¿Está seguro de validar todos los documentos de ${this.selectedDocumento.nombreCompleto}?`)) {
      this.selectedDocumento.estado = 'validado';
      this.calcularEstadisticas();
      this.applyFilters();
      this.closeDocumentosModal();
      alert('Todos los documentos han sido validados');
    }
  }

  rechazarTodosDocumentos(): void {
    this.showDocumentosModal = false;
    if (this.selectedDocumento) {
      this.showRechazarModal = true;
    }
  }

  exportarReporte(): void {
    alert('Exportando reporte de documentos...');
    // Aquí iría la lógica para generar y descargar el reporte
  }

  // Modales
  closeDocumentosModal(): void {
    this.showDocumentosModal = false;
    this.selectedDocumento = null;
  }

  closeRechazarModal(): void {
    this.showRechazarModal = false;
    this.motivoRechazo = '';
  }
}
