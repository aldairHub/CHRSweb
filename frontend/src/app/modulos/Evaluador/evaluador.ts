import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router,RouterModule } from '@angular/router';

// Asegúrate que las rutas a tus componentes compartidos sean correctas
// A veces es '.../navbar/navbar.component' dependiendo de tu estructura
import { NavbarComponent } from '../../component/navbar';
import { FooterComponent } from '../../component/footer';

// Interfaz para definir la estructura de las tarjetas del menú
interface ModuloEvaluador {
  titulo: string;
  descripcion: string;
  icono: string;
  ruta: string;
  claseColor: string; // Para estilos personalizados si los necesitas
}

@Component({
  selector: 'app-evaluador',
  standalone: true,
  imports: [CommonModule, NavbarComponent, FooterComponent],
  templateUrl: './evaluador.html', // Asegúrate que coincida el nombre
  styleUrls: ['./evaluador.scss']  // Asegúrate que coincida el nombre
})
export class EvaluadorComponent {

  // === LISTA DE MÓDULOS DEL SISTEMA ===
  // Basado en tu Base de Datos y el flujo de selección docente
  modulos: ModuloEvaluador[] = [
    {
      titulo: 'Solicitar Docente',
      descripcion: 'Crear nueva solicitud de personal académico según necesidades de la carrera.',
      icono: 'pi pi-user-plus', // Icono de PrimeIcons
      ruta: 'solicitar',
      claseColor: 'bg-green-100'
    },
    {
      titulo: 'Gestión de Postulantes',
      descripcion: 'Revisar lista de candidatos, hojas de vida y validar documentos.',
      icono: 'pi pi-users',
      ruta: 'postulantes',
      claseColor: 'bg-blue-100'
    },
    {
      titulo: 'Evaluación de Méritos',
      descripcion: 'Calificar formación académica, experiencia y publicaciones (Tabla evaluacion_meritos).',
      icono: 'pi pi-file-edit',
      ruta: 'evaluacion-meritos', // Ruta futura
      claseColor: 'bg-orange-100'
    },
    {
      titulo: 'Entrevistas',
      descripcion: 'Programar y calificar entrevistas presenciales o virtuales (Tabla entrevista).',
      icono: 'pi pi-comments',
      ruta: 'entrevistas', // Ruta futura
      claseColor: 'bg-purple-100'
    },
    {
      titulo: 'Resultados e Informes',
      descripcion: 'Generar actas finales y visualizar ganadores del concurso (Tabla informe_final).',
      icono: 'pi pi-chart-bar',
      ruta: 'resultados', // Ruta futura
      claseColor: 'bg-teal-100'
    }
  ];

  constructor(private router: Router) {}

  // Método genérico para navegar
  navegarA(ruta: string): void {
    console.log('Navegando al módulo:', ruta);
    // Navega relativo a la ruta actual: /evaluador/solicitar, /evaluador/postulantes, etc.
    this.router.navigate([`/evaluador/${ruta}`]);
  }
}
