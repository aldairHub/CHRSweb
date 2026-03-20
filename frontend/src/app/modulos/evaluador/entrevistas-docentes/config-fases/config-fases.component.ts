// ══════════════════════════════════════════════════════════════
// config-fases.component.ts — EVALUADOR (solo lectura + asignar evaluadores)
// REEMPLAZA el archivo actual
// ══════════════════════════════════════════════════════════════
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { EntrevistasEstadoService } from '../../../../services/entrevistas/entrevistas-estado.service';
import { ModalEvaluadoresComponent } from '../../../../component/modal-evaluadores.component';

interface FaseDTO {
  idFase: number;
  nombre: string;
  tipo: string;
  peso: number;
  orden: number;
  estado: boolean;
  nombrePlantilla?: string;
  evaluadoresPermitidos: string[];
}

interface PostulanteConProceso {
  idProceso: number;
  nombres: string;
  apellidos: string;
}

@Component({
  selector: 'app-config-fases',
  standalone: true,
  imports: [CommonModule, RouterModule, ModalEvaluadoresComponent],
  templateUrl: './config-fases.component.html',
  styleUrls: ['./config-fases.component.scss']
})
export class ConfigFasesComponent implements OnInit {

  private readonly API = 'http://localhost:8080/api';

  fases: FaseDTO[] = [];
  postulantes: PostulanteConProceso[] = [];
  isLoading = false;
  error = '';
  idSolicitud = 0;

  // Modal evaluadores
  modalVisible = false;
  procesoSeleccionado: PostulanteConProceso | null = null;
  faseSeleccionada: FaseDTO | null = null;

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
    private estadoService: EntrevistasEstadoService
  ) {}

  ngOnInit(): void {
    this.idSolicitud = this.estadoService.getIdSolicitud() || 0;
    this.cargarFases();
    if (this.idSolicitud) this.cargarPostulantes();
  }

  cargarFases(): void {
    this.isLoading = true;
    this.http.get<FaseDTO[]>('http://localhost:8080/api/evaluacion/fases').subscribe({
      next: (data) => { this.fases = data; this.isLoading = false; },
      error: () => { this.error = 'Error al cargar fases.'; this.isLoading = false; }
    });
  }

  cargarPostulantes(): void {
    this.http.get<any>(`${this.API}/matriz-meritos/solicitud/${this.idSolicitud}`).subscribe({
      next: (data) => {
        this.postulantes = (data.candidatos || []).map((c: any) => ({
          idProceso: c.idProceso,
          nombres:   c.nombres,
          apellidos: c.apellidos
        }));
      },
      error: () => {}
    });
  }

  abrirModalEvaluadores(f: FaseDTO, p: PostulanteConProceso): void {
    this.faseSeleccionada    = f;
    this.procesoSeleccionado = p;
    this.modalVisible        = true;
  }

  cerrarModal(): void {
    this.modalVisible        = false;
    this.procesoSeleccionado = null;
    this.faseSeleccionada    = null;
  }

  get contextLabel(): string {
    if (!this.procesoSeleccionado || !this.faseSeleccionada) return '';
    return `${this.procesoSeleccionado.apellidos} ${this.procesoSeleccionado.nombres} — ${this.faseSeleccionada.nombre}`;
  }

  get pesoTotal(): number { return this.fases.reduce((s, f) => s + (f.peso || 0), 0); }
  get pesoValido(): boolean { return this.pesoTotal === 100; }

  getTipoLabel(tipo: string): string {
    const m: Record<string, string> = { automatica: 'Automática', reunion: 'Reunión', practica: 'Práctica', decision: 'Decisión' };
    return m[tipo] ?? tipo;
  }

  getTipoBadge(tipo: string): string {
    const m: Record<string, string> = { automatica: 'info', reunion: 'success', practica: 'warning', decision: 'purple' };
    return m[tipo] ?? 'default';
  }

  navegarPostulantes(): void {
    if (this.idSolicitud) {
      this.router.navigate(['/evaluador/entrevistas-docentes/postulantes', this.idSolicitud]);
    }
  }

  esRutaActiva(segmento: string): boolean {
    return this.router.url.includes(segmento);
  }
}
