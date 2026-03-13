import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { DashboardService } from '../../../../services/entrevistas/dashboard.service';
import { PostulantesService } from '../../../../services/entrevistas/postulantes.service';
import { ReunionesService } from '../../../../services/entrevistas/reuniones.service';
import { DashboardStats, PostulanteResumen, ReunionResumen } from '../../../../models/entrevistas-models';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-evaluacion-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class EvaluacionDashboardComponent implements OnInit {

  stats: DashboardStats = {
    postulantesActivos: 0,
    reunionesProgramadas: 0,
    evaluacionesCompletas: 0,
    pendientesHoy: 0
  };

  postulantesRecientes: PostulanteResumen[] = [];
  reunionesProximas: ReunionResumen[] = [];
  isLoading = true;
  error = '';

  // Mapa idProceso → próxima reunión (para mostrar en la tabla)
  private reunionesPorProceso = new Map<number, ReunionResumen>();

  constructor(
    private router: Router,
    private cdr: ChangeDetectorRef,
    private dashboardService: DashboardService,
    private postulantesService: PostulantesService,
    private reunionesService: ReunionesService
  ) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.isLoading = true;
    this.error = '';
    forkJoin({
      stats:       this.dashboardService.obtenerStats(),
      postulantes: this.postulantesService.listar('en_proceso'),
      reuniones:   this.reunionesService.listarProgramadas()
    }).subscribe({
      next: ({ stats, postulantes, reuniones }) => {
        this.stats                = stats;
        this.postulantesRecientes = postulantes.slice(0, 5);
        this.reunionesProximas    = reuniones.slice(0, 5);

        // Mapear reuniones por proceso para consulta rápida en la tabla
        this.reunionesPorProceso.clear();
        reuniones.forEach(r => {
          if (r.idProceso && !this.reunionesPorProceso.has(r.idProceso)) {
            this.reunionesPorProceso.set(r.idProceso, r);
          }
        });

        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error     = 'No se pudo cargar la información. Intente nuevamente.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  navegarPostulante(idProceso: number): void {
    this.router.navigate(['/evaluador/entrevistas-docentes/postulantes', idProceso]);
  }

  navegarPostulantes(): void {
    this.router.navigate(['/evaluador/entrevistas-docentes/postulantes']);
  }

  getNombreCompleto(p: PostulanteResumen): string {
    return `${p.nombres} ${p.apellidos}`;
  }

  /** Devuelve "fecha hora" de la próxima reunión del postulante, o null si no tiene */
  getProximaReunion(p: PostulanteResumen): string | null {
    const r = this.reunionesPorProceso.get(p.idProceso);
    if (!r) return null;
    return `${r.fecha} ${r.hora}`;
  }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      en_proceso: 'warning', completado: 'success', rechazado: 'danger', pendiente: 'pending'
    };
    return map[estado] ?? 'default';
  }

  getBadgeLabel(estado: string): string {
    const map: Record<string, string> = {
      en_proceso: 'En Proceso', completado: 'Completado', rechazado: 'Rechazado', pendiente: 'Pendiente'
    };
    return map[estado] ?? estado;
  }

  getModalidadLabel(m: string): string {
    const map: Record<string, string> = {
      zoom: 'Zoom', meet: 'Google Meet', teams: 'Teams', presencial: 'Presencial'
    };
    return map[m] ?? m;
  }

  iniciarReunion(enlace: string | undefined): void {
    if (enlace) window.open(enlace, '_blank');
  }
}
