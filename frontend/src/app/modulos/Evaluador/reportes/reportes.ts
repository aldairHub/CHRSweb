import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // <--- OBLIGATORIO para que funcione *ngFor
import { NavbarComponent } from '../../../component/navbar'; // Ajusta la ruta si es necesario
import { FooterComponent } from '../../../component/footer'; // Ajusta la ruta si es necesario

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [
    CommonModule,      // Sin esto, el *ngFor da error
    NavbarComponent,   // Para <app-navbar>
    FooterComponent    // Para <app-footer>
  ],
  templateUrl: './reportes.html', // Asegúrate que tu archivo HTML se llame así
  styleUrls: ['./reportes.scss']
})
export class ReportesComponent {

  // 1. Datos para los cuadros de arriba (KPIs)
  kpis = [
    { titulo: 'Total Postulantes', valor: 45, icono: '👥', color: 'blue' },
    { titulo: 'Aprobados', valor: 28, icono: '✅', color: 'green' },
    { titulo: 'Rechazados', valor: 12, icono: '❌', color: 'red' },
    { titulo: 'Pendientes', valor: 5, icono: '⏳', color: 'orange' }
  ];

  // 2. Datos para la lista de archivos (PDF/Excel)
  reportesRecientes = [
    { nombre: 'Consolidado_2025.pdf', fecha: '08/02/2025', peso: '2.4 MB', tipo: 'pdf' },
    { nombre: 'Nomina_Sistemas.xlsx', fecha: '07/02/2025', peso: '1.1 MB', tipo: 'excel' },
    { nombre: 'Acta_Fase1.pdf', fecha: '05/02/2025', peso: '850 KB', tipo: 'pdf' },
    { nombre: 'Asistencia.xlsx', fecha: '01/02/2025', peso: '500 KB', tipo: 'excel' },
  ];

  // 3. Datos para las gráficas de barras (Lo que te daba error en la foto)
  rendimientoPorArea = [
    { area: 'Ingeniería de Software', puntaje: 85, color: '#1B5E20' }, // Verde oscuro
    { area: 'Redes y Telecom.', puntaje: 60, color: '#43a047' },       // Verde medio
    { area: 'Base de Datos', puntaje: 75, color: '#2e7d32' },          // Verde normal
    { area: 'Inteligencia Art.', puntaje: 40, color: '#66bb6a' }       // Verde claro
  ];

  constructor() {}

  // Función para el botón de imprimir
  imprimir() {
    window.print();
  }

  // Función simulada de descarga
  descargar(archivo: string) {
    alert('Descargando: ' + archivo);
  }
}
