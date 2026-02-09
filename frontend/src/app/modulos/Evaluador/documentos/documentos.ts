import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // Para *ngFor, *ngIf
import { FormsModule } from '@angular/forms';   // Para ngModel
import { Router } from '@angular/router';

// 1. IMPORTA TUS COMPONENTES (Ajusta la ruta si te sale error de ruta)
// Si tu carpeta 'component' está en 'src/app/component', la ruta relativa desde aquí es algo así:
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';
// NOTA: Si te marca error en la ruta, intenta borrando la ruta y escribiendo
// "NavbarComponent" para que VS Code te sugiera la importación automática.

@Component({
  selector: 'app-documentos',
  standalone: true,
  // 2. AGRÉGALOS AQUÍ EN EL ARRAY IMPORTS (Esto es lo que te falta)
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent, // <--- OBLIGATORIO
    FooterComponent  // <--- OBLIGATORIO
  ],
  templateUrl: './documentos.html',
  styleUrls: ['./documentos.scss'] // O .css, según uses
})
export class DocumentosComponent {

  postulante = {
    nombres: 'Juan Carlos Pérez Loor',
    cedula: '1205489632',
    cargo: 'Docente Tiempo Completo - Ingeniería Software'
  };

  documentos = [
    {
      id: 1,
      nombre: 'Cédula de Identidad',
      archivo: 'cedula_jperez.pdf',
      estado: 'pendiente',
      observacion: ''
    },
    {
      id: 2,
      nombre: 'Título de Tercer Nivel',
      archivo: 'titulo_grado.pdf',
      estado: 'pendiente',
      observacion: ''
    },
    {
      id: 3,
      nombre: 'Certificado de Experiencia Laboral',
      archivo: 'cert_trabajo.pdf',
      estado: 'rechazado',
      observacion: 'El certificado no tiene firma electrónica.'
    },
    {
      id: 4,
      nombre: 'Récord Policial',
      archivo: 'record.pdf',
      estado: 'valido',
      observacion: ''
    }
  ];

  constructor(private router: Router) {}

  volver() {
    this.router.navigate(['/evaluador/postulantes']);
  }

  validarDocumento(doc: any, estado: string) {
    doc.estado = estado;
    if (estado === 'valido') {
      doc.observacion = '';
    }
  }

  guardarRevision() {
    const pendientes = this.documentos.filter(d => d.estado === 'pendiente');

    if (pendientes.length > 0) {
      alert(`Aún tienes ${pendientes.length} documentos sin revisar.`);
      return;
    }

    console.log('Enviando revisión:', this.documentos);
    alert('Validación guardada correctamente.');
    this.volver();
  }

  verArchivo(nombreArchivo: string) {
    alert(`Abriendo visor para: ${nombreArchivo}`);
  }
}
