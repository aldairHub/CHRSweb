import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

// AJUSTA ESTAS RUTAS SEGÚN TU CARPETA DE COMPONENTES
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';

@Component({
  selector: 'app-solicitar-docente',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './solicitar-docente.html',
  styleUrls: ['./solicitar-docente.scss']
})
export class SolicitarDocenteComponent {

  solicitud = {
    id_carrera: '',
    id_area: '',
    id_materia: '',
    nivel_academico: '',
    cantidad_docentes: 1,
    experiencia_profesional_min: 0,
    experiencia_docente_min: 0,
    justificacion: '',
    observaciones: ''
  };

  constructor(private router: Router) {}

  guardarSolicitud() {
    console.log('Enviando solicitud:', this.solicitud);
    // Aquí tu lógica de servicio
    alert('Solicitud guardada correctamente (Simulación)');
  }

  cancelar() {
    this.router.navigate(['/evaluador']);
  }
}
