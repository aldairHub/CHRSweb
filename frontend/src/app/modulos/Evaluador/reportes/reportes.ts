import { Component, OnInit,ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';
import { HttpClient } from '@angular/common/http';

interface SolicitudDto {
  idSolicitud: number;
  estadoSolicitud: string;
  fechaSolicitud: string;
  cantidadDocentes: number;
}

interface ReporteView {
  id: number;
  nombre: string;
  fecha: string;
  tipo: string;
}

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, NavbarComponent, FooterComponent],
  templateUrl: './reportes.html',
  styleUrls: ['./reportes.scss']
})
export class ReportesComponent implements OnInit {

  reportesRecientes: ReporteView[] = [];

  // ✅ BASE API CORRECTA
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient,private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargarSolicitudes();
  }

  // 🔥 CARGAR DESDE BD (FIXED)
  cargarSolicitudes() {
    this.http.get<SolicitudDto[]>(`${this.apiUrl}/solicitudes-docente`)
      .subscribe({
        next: (data: SolicitudDto[]) => {

          this.reportesRecientes = data.map(s => ({
            id: s.idSolicitud,
            nombre: `Solicitud #${s.idSolicitud}`,
            fecha: new Date(s.fechaSolicitud).toLocaleDateString(),
            tipo: 'pdf'
          }));
          this.cdr.detectChanges();
          console.log('Solicitudes:', this.reportesRecientes);
        },
        error: (err: any) => {
          console.error('Error cargando solicitudes', err);
        }
      });
  }

  imprimir() {
    window.print();
  }

  // 👁 VER PDF EN NUEVA PESTAÑA
  ver(id: number) {
    window.open(`${this.apiUrl}/solicitudes-docente/${id}/reporte-pdf`, '_blank');
  }




  // ⬇️ DESCARGAR PDF (MISMO ENDPOINT)
  descargar(id: number) {
    this.http.get(
      `${this.apiUrl}/solicitudes-docente/${id}/reporte-pdf`,
      { responseType: 'blob' }
    ).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `solicitud-${id}.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }


}
