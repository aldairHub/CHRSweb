import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { NavbarComponent } from '../../../../component/navbar';

interface SesionActiva {
  id_sesion: number;
  usuario_app: string;
  ip_cliente: string;
  fecha_login: string;
  minutos_activo: number;
  user_agent: string;
}

@Component({
  selector: 'app-sesiones-activas',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './sesiones-activas.html',
  styleUrls: ['./sesiones-activas.scss']
})
export class SesionesActivasComponent implements OnInit, OnDestroy {

  private readonly api = '/api/admin/auditoria'; //
  sesiones: SesionActiva[] = [];
  isLoading = false;
  cierreEnProceso: string | null = null;
  mensaje: string | null = null;
  private intervalId: any;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef //
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.intervalId = setInterval(() => this.cargar(), 30000);
  }

  ngOnDestroy(): void {
    if (this.intervalId) clearInterval(this.intervalId);
  }

  cargar(): void {
    this.isLoading = true;
    this.http.get<SesionActiva[]>(`${this.api}/sesiones/activas`).subscribe({
      next: (data) => {
        this.sesiones = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  forzarCierre(usuarioApp: string): void {
    if (!confirm(`¿Cerrar sesión de ${usuarioApp}?`)) return;
    this.cierreEnProceso = usuarioApp;
    this.http.post<any>(`${this.api}/sesiones/${usuarioApp}/forzar-cierre`, {}).subscribe({
      next: (res) => {
        this.mensaje = res.mensaje;
        this.cierreEnProceso = null;
        this.cargar();
        setTimeout(() => { this.mensaje = null; this.cdr.detectChanges(); }, 4000);
        this.cdr.detectChanges();
      },
      error: () => {
        this.cierreEnProceso = null;
        this.cdr.detectChanges();
      }
    });
  }

  formatMin(min: number): string {
    if (min < 60) return `${Math.floor(min)} min`;
    return `${Math.floor(min / 60)}h ${Math.floor(min % 60)}m`;
  }
}
