// import { Component } from '@angular/core';
// import { CommonModule } from '@angular/common';
// import { Router,RouterModule } from '@angular/router';
//
// // Asegúrate que las rutas a tus componentes compartidos sean correctas
// // A veces es '.../navbar/navbar.component' dependiendo de tu estructura
// import { NavbarComponent } from '../../component/navbar';
// import { FooterComponent } from '../../component/footer';
//
// // Interfaz para definir la estructura de las tarjetas del menú
// interface ModuloEvaluador {
//   titulo: string;
//   descripcion: string;
//   icono: string;
//   ruta: string;
//   claseColor: string; // Para estilos personalizados si los necesitas
// }
//
// @Component({
//   selector: 'app-evaluador',
//   standalone: true,
//   imports: [CommonModule, NavbarComponent, FooterComponent],
//   templateUrl: './evaluador.html', // Asegúrate que coincida el nombre
//   styleUrls: ['./evaluador.scss']  // Asegúrate que coincida el nombre
// })
// export class EvaluadorComponent {
//
//   // === LISTA DE MÓDULOS DEL SISTEMA ===
//   // Basado en tu Base de Datos y el flujo de selección docente
//   modulos: ModuloEvaluador[] = [
//     {
//       titulo: 'Solicitar Docente',
//       descripcion: 'Crear nueva solicitud de personal académico según necesidades de la carrera.',
//       icono: 'pi pi-user-plus', // Icono de PrimeIcons
//       ruta: 'solicitar',
//       claseColor: 'bg-green-100'
//     },
//     {
//       titulo: 'Gestión de Postulantes',
//       descripcion: 'Revisar lista de candidatos, hojas de vida y validar documentos.',
//       icono: 'pi pi-users',
//       ruta: 'postulantes',
//       claseColor: 'bg-blue-100'
//     },
//     {
//       titulo: 'Evaluación de Méritos',
//       descripcion: 'Calificar formación académica, experiencia y publicaciones (Tabla evaluacion_meritos).',
//       icono: 'pi pi-file-edit',
//       ruta: 'evaluacion-meritos', // Ruta futura
//       claseColor: 'bg-orange-100'
//     },
//     {
//       titulo: 'Entrevistas',
//       descripcion: 'Programar y calificar entrevistas presenciales o virtuales (Tabla entrevista).',
//       icono: 'pi pi-comments',
//       ruta: 'entrevistas', // Ruta futura
//       claseColor: 'bg-purple-100'
//     },
//     {
//       titulo: 'Resultados e Informes',
//       descripcion: 'Generar actas finales y visualizar ganadores del concurso (Tabla informe_final).',
//       icono: 'pi pi-chart-bar',
//       ruta: 'resultados', // Ruta futura
//       claseColor: 'bg-teal-100'
//     }
//   ];
//
//   constructor(private router: Router) {}
//
//   // Método genérico para navegar
//   navegarA(ruta: string): void {
//     console.log('Navegando al módulo:', ruta);
//     // Navega relativo a la ruta actual: /evaluador/solicitar, /evaluador/postulantes, etc.
//     this.router.navigate([`/evaluador/${ruta}`]);
//   }
// }
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../component/navbar';
import { FooterComponent } from '../../component/footer';
import { AuthService } from '../../services/auth.service';

interface DashCard {
  titulo:      string;
  descripcion: string;
  ruta:        string;
  svgPaths:    Array<{ d: string }>;
}

// ─── Mapa ruta → SVG paths (extraídos del evaluador.html original) ────────────
// La clave es el último segmento de opcion.ruta:
// "/evaluador/postulantes" → "postulantes"
const SVG_MAP: Record<string, Array<{ d: string }>> = {

  'postulantes': [
    { d: 'M21.3334 28V25.3333C21.3334 23.9188 20.7715 22.5623 19.7713 21.5621C18.7711 20.5619 17.4146 20 16.0001 20H8.00008C6.58559 20 5.22904 20.5619 4.22885 21.5621C3.22865 22.5623 2.66675 23.9188 2.66675 25.3333V28' },
    { d: 'M12.0001 14.6667C14.9456 14.6667 17.3334 12.2789 17.3334 9.33333C17.3334 6.38781 14.9456 4 12.0001 4C9.05456 4 6.66675 6.38781 6.66675 9.33333C6.66675 12.2789 9.05456 14.6667 12.0001 14.6667Z' },
    { d: 'M29.3333 28V25.3333C29.3324 24.1516 28.9391 23.0037 28.2151 22.0698C27.4911 21.1358 26.4774 20.4688 25.3333 20.1733' },
    { d: 'M21.3333 4.17334C22.4805 4.46707 23.4973 5.13427 24.2234 6.06975C24.9496 7.00523 25.3437 8.15578 25.3437 9.34001C25.3437 10.5242 24.9496 11.6748 24.2234 12.6103C23.4973 13.5457 22.4805 14.2129 21.3333 14.5067' },
  ],

  'documentos': [
    { d: 'M19.9999 2.66666H7.99992C7.29267 2.66666 6.6144 2.94761 6.1143 3.4477C5.6142 3.9478 5.33325 4.62608 5.33325 5.33332V26.6667C5.33325 27.3739 5.6142 28.0522 6.1143 28.5523C6.6144 29.0524 7.29267 29.3333 7.99992 29.3333H23.9999C24.7072 29.3333 25.3854 29.0524 25.8855 28.5523C26.3856 28.0522 26.6666 27.3739 26.6666 26.6667V9.33332L19.9999 2.66666Z' },
    { d: 'M18.6667 2.66666V7.99999C18.6667 8.70723 18.9477 9.38551 19.4478 9.88561C19.9479 10.3857 20.6262 10.6667 21.3334 10.6667H26.6667' },
    { d: 'M13.3334 12H10.6667' },
    { d: 'M21.3334 17.3333H10.6667' },
    { d: 'M21.3334 22.6667H10.6667' },
  ],

  'evaluacion': [
    { d: 'M4 4V25.3333C4 26.0406 4.28095 26.7189 4.78105 27.219C5.28115 27.719 5.95942 28 6.66667 28H28' },
    { d: 'M24 22.6667V12' },
    { d: 'M17.3333 22.6667V6.66666' },
    { d: 'M10.6667 22.6667V18.6667' },
  ],

  'reportes': [
    { d: 'M28.56 14.5627C28.7987 14.4574 29.0012 14.2844 29.1426 14.0651C29.2839 13.8458 29.3578 13.5899 29.3551 13.329C29.3524 13.0681 29.2732 12.8138 29.1274 12.5975C28.9815 12.3811 28.7755 12.2123 28.5346 12.112L17.1066 6.90667C16.7592 6.74821 16.3818 6.6662 16 6.6662C15.6181 6.6662 15.2407 6.74821 14.8933 6.90667L3.46664 12.1067C3.22927 12.2106 3.02733 12.3815 2.88553 12.5984C2.74373 12.8153 2.66821 13.0689 2.66821 13.328C2.66821 13.5872 2.74373 13.8407 2.88553 14.0576C3.02733 14.2745 3.22927 14.4454 3.46664 14.5493L14.8933 19.76C15.2407 19.9185 15.6181 20.0005 16 20.0005C16.3818 20.0005 16.7592 19.9185 17.1066 19.76L28.56 14.5627Z' },
    { d: 'M29.3333 13.3333V21.3333' },
    { d: 'M8 16.6667V21.3333C8 22.3942 8.84286 23.4116 10.3431 24.1618C11.8434 24.9119 13.8783 25.3333 16 25.3333C18.1217 25.3333 20.1566 24.9119 21.6569 24.1618C23.1571 23.4116 24 22.3942 24 21.3333V16.6667' },
  ],

  'solicitar': [
    { d: 'M21.3333 28V25.3333C21.3333 23.9188 20.7714 22.5623 19.7712 21.5621C18.771 20.5619 17.4145 20 16 20H8.00004C6.58555 20 5.22909 20.5619 4.22889 21.5621C3.2287 22.5623 2.66671 23.9188 2.66671 25.3333V28' },
    { d: 'M12 14.6667C14.9455 14.6667 17.3333 12.2789 17.3333 9.33333C17.3333 6.38781 14.9455 4 12 4C9.05446 4 6.66667 6.38781 6.66667 9.33333C6.66667 12.2789 9.05446 14.6667 12 14.6667Z' },
    { d: 'M26.6667 14.6667V7.33333' },
    { d: 'M23 11H30.3333' },
  ],

  'entrevistas': [
    { d: 'M4 5.33333C4 4.62609 4.28095 3.94781 4.78105 3.44772C5.28115 2.94762 5.95942 2.66666 6.66667 2.66666H25.3333C26.0406 2.66666 26.7189 2.94762 27.219 3.44772C27.719 3.94781 28 4.62609 28 5.33333V18.6667C28 19.3739 27.719 20.0522 27.219 20.5523C26.7189 21.0524 26.0406 21.3333 25.3333 21.3333H12L6.66667 26.6667V21.3333H6.66667C5.95942 21.3333 5.28115 21.0524 4.78105 20.5523C4.28095 20.0522 4 19.3739 4 18.6667V5.33333Z' },
    { d: 'M10 10.6667H22' },
    { d: 'M10 15.3333H18' },
  ],

};

const SVG_FALLBACK: Array<{ d: string }> = [
  { d: 'M16 2.6667C8.6364 2.6667 2.6667 8.6364 2.6667 16C2.6667 23.3636 8.6364 29.3333 16 29.3333C23.3636 29.3333 29.3333 23.3636 29.3333 16C29.3333 8.6364 23.3636 2.6667 16 2.6667Z' },
  { d: 'M16 10.6667V16' },
  { d: 'M16 21.3333H16.0133' },
];

@Component({
  selector: 'app-evaluador',
  standalone: true,
  imports: [CommonModule, NavbarComponent, FooterComponent],
  templateUrl: './evaluador.html',
  styleUrls: ['./evaluador.scss']
})
export class EvaluadorComponent implements OnInit {

  cards: DashCard[] = [];

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.construirCards();
  }

  private construirCards(): void {
    const modulo = this.authService.getModulo();
    if (!modulo?.opciones?.length) {
      this.cards = [];
      return;
    }

    this.cards = modulo.opciones.map(op => {
      // "/evaluador/postulantes" → "postulantes"
      const rutaKey = (op.ruta || '').split('/').pop() ?? '';

      return {
        titulo:      op.nombre,
        descripcion: op.descripcion || '',
        ruta:        rutaKey,
        svgPaths:    SVG_MAP[rutaKey] ?? SVG_FALLBACK,
      };
    });
  }

  navegarA(ruta: string): void {
    this.router.navigate([`/evaluador/${ruta}`]);
  }
}
