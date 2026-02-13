import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';

// Importar el servicio
import { PrepostulacionService, Prepostulacion, DocumentosResponse } from '../../../services/prepostulacion.service';

// Importar componentes
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';

// Interfaz para los documentos en la vista
interface Documento {
  id: number;
  nombre: string;
  archivo: string;
  urlCompleta: string;
  estado: 'pendiente' | 'valido' | 'rechazado';
  observacion: string;
  tipoDocumento: 'cedula' | 'foto' | 'prerrequisitos';
}

@Component({
  selector: 'app-documentos',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    HttpClientModule,
    NavbarComponent,
    FooterComponent
  ],
  providers: [PrepostulacionService],
  templateUrl: './documentos.html',
  styleUrls: ['./documentos.scss']
})
export class DocumentosComponent implements OnInit {

  // ID de la prepostulación (viene de la ruta)
  prepostulacionId!: number;

  // Datos del postulante
  postulante = {
    nombres: '',
    cedula: '',
    cargo: 'Docente Tiempo Completo - Ingeniería Software'
  };

  // Lista de documentos para validar
  documentos: Documento[] = [];

  // Estado de carga
  cargando = true;
  error: string | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private prepostulacionService: PrepostulacionService
  ) {}

  ngOnInit() {
    // Obtener el ID de la prepostulación desde la ruta
    // Ejemplo: /evaluador/documentos/123
    this.route.params.subscribe(params => {
      this.prepostulacionId = +params['id']; // El + convierte string a number
      this.cargarDatos();
    });
  }

  /**
   * Cargar los datos de la prepostulación desde el backend
   */
  cargarDatos() {
    this.cargando = true;
    this.error = null;

    // Primero obtenemos los datos básicos de la prepostulación
    this.prepostulacionService.obtenerPrepostulacion(this.prepostulacionId)
      .subscribe({
        next: (prepostulacion: Prepostulacion) => {
          // Llenar datos del postulante
          this.postulante.nombres = `${prepostulacion.nombres} ${prepostulacion.apellidos}`;
          this.postulante.cedula = prepostulacion.identificacion;

          // Ahora obtenemos las URLs de los documentos
          this.cargarDocumentos();
        },
        error: (err) => {
          console.error('Error al cargar prepostulación:', err);
          this.error = 'No se pudo cargar la información del postulante';
          this.cargando = false;
        }
      });
  }

  /**
   * Cargar las URLs de los documentos desde el backend
   */
  cargarDocumentos() {
    this.prepostulacionService.obtenerDocumentos(this.prepostulacionId)
      .subscribe({
        next: (docs: DocumentosResponse) => {
          // Crear la lista de documentos para la validación
          this.documentos = [
            {
              id: 1,
              nombre: 'Cédula de Identidad',
              archivo: 'cedula.pdf',
              urlCompleta: docs.cedula,
              estado: 'pendiente',
              observacion: '',
              tipoDocumento: 'cedula'
            },
            {
              id: 2,
              nombre: 'Fotografía',
              archivo: 'foto.jpg',
              urlCompleta: docs.foto,
              estado: 'pendiente',
              observacion: '',
              tipoDocumento: 'foto'
            },
            {
              id: 3,
              nombre: 'Prerrequisitos',
              archivo: 'prerrequisitos.pdf',
              urlCompleta: docs.prerrequisitos,
              estado: 'pendiente',
              observacion: '',
              tipoDocumento: 'prerrequisitos'
            }
          ];

          this.cargando = false;
        },
        error: (err) => {
          console.error('Error al cargar documentos:', err);
          this.error = 'No se pudieron cargar los documentos';
          this.cargando = false;
        }
      });
  }

  /**
   * Volver a la lista de postulantes
   */
  volver() {
    this.router.navigate(['/evaluador/postulantes']);
  }

  /**
   * Validar o rechazar un documento
   */
  validarDocumento(doc: Documento, estado: 'valido' | 'rechazado') {
    doc.estado = estado;
    if (estado === 'valido') {
      doc.observacion = '';
    }
  }

  /**
   * Guardar la revisión y enviarla al backend
   */
  guardarRevision() {
    // Validar que todos los documentos estén revisados
    const pendientes = this.documentos.filter(d => d.estado === 'pendiente');

    if (pendientes.length > 0) {
      alert(`Aún tienes ${pendientes.length} documentos sin revisar.`);
      return;
    }

    // Determinar el estado general de la prepostulación
    const tieneRechazados = this.documentos.some(d => d.estado === 'rechazado');
    const estadoFinal = tieneRechazados ? 'RECHAZADO' : 'APROBADO';

    // Juntar todas las observaciones
    const observaciones = this.documentos
      .filter(d => d.observacion.trim() !== '')
      .map(d => `${d.nombre}: ${d.observacion}`)
      .join(' | ');

    // Preparar el request
    const request = {
      estado: estadoFinal,
      observaciones: observaciones || 'Documentos revisados correctamente',
      idRevisor: 1 // ⚠️ TODO: Obtener el ID del usuario logueado
    };

    // Enviar al backend
    this.prepostulacionService.actualizarEstado(this.prepostulacionId, request)
      .subscribe({
        next: (response) => {
          console.log('Respuesta del servidor:', response);
          alert('✅ Validación guardada correctamente.');
          this.volver();
        },
        error: (err) => {
          console.error('Error al guardar validación:', err);
          alert('❌ Error al guardar la validación. Intenta nuevamente.');
        }
      });
  }

  /**
   * Abrir el archivo en una nueva pestaña
   */
  verArchivo(urlCompleta: string) {
    if (!urlCompleta) {
      alert('No hay archivo disponible para este documento.');
      return;
    }

    // Abrir en nueva pestaña
    window.open(urlCompleta, '_blank');
  }
}
