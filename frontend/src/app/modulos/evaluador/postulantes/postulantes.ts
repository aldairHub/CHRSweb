import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

// AJUSTA ESTAS RUTAS SI TUS ARCHIVOS TIENEN OTROS NOMBRES
// Normalmente es: ../../../component/navbar/navbar.component
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';

// Interfaz basada en tu BD 'public.postulante'
export interface Postulante {
  id_postulante: number;
  identificacion: string;
  nombres_postulante: string;
  apellidos_postulante: string;
  correo_postulante: string;
  telefono_postulante: string;
  estado: 'pendiente' | 'aprobado' | 'rechazado';
}

@Component({
  selector: 'app-postulantes',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, FooterComponent],
  templateUrl: './postulantes.html', // Asegúrate que tu HTML se llame así
  styleUrls: ['./postulantes.scss']  // Asegúrate que tu SCSS se llame así
})
export class PostulantesComponent {

  // Filtros
  filtro = {
    cedula: '',
    apellido: ''
  };

  // Variables para controlar el Modal y UI
  mostrarModal = false;
  correoSeleccionado = '';

  // Datos simulados (Aquí luego conectarás con tu Backend Service)
  postulantes: Postulante[] = [
    {
      id_postulante: 1,
      identificacion: '1205489632',
      nombres_postulante: 'Juan Carlos',
      apellidos_postulante: 'Pérez Loor',
      correo_postulante: 'jperez@uteq.edu.ec',
      telefono_postulante: '0998541236',
      estado: 'pendiente'
    },
    {
      id_postulante: 2,
      identificacion: '1302569874',
      nombres_postulante: 'Maria Elena',
      apellidos_postulante: 'Gómez Sanchez',
      correo_postulante: 'mgomez@gmail.com',
      telefono_postulante: '0987456321',
      estado: 'aprobado'
    }
  ];

  constructor(private router: Router) {}

  // Navegación
  volver() {
    this.router.navigate(['/evaluador']);
  }

  // Lógica de Filtros
  buscar() {
    console.log('Filtrando por:', this.filtro);
    // Lógica futura: this.postulanteService.filtrar(this.filtro)...
  }

  // Ver detalles
  verPerfil(postulante: Postulante) {
    console.log('Viendo perfil de:', postulante.nombres_postulante);
    // this.router.navigate(['/evaluador/detalle-postulante', postulante.id_postulante]);
  }

  // === Lógica del Modal (Pop-up) ===
  abrirModalExito() {
    // Aquí podrías tomar el correo real del postulante seleccionado
    this.correoSeleccionado = 'ejemplo@correo.com';
    this.mostrarModal = true;
  }

  cerrarModal() {
    this.mostrarModal = false;
  }
}
