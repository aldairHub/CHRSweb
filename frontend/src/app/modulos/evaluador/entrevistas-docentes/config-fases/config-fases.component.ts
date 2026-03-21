import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { EntrevistasEstadoService } from '../../../../services/entrevistas/entrevistas-estado.service';

interface FaseDTO {
  idFase: number;
  nombre: string;
  tipo: string;
  peso: number;
  orden: number;
  estado: boolean;
  nombrePlantilla?: string;
}

@Component({
  selector: 'app-config-fases',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './config-fases.component.html',
  styleUrls: ['./config-fases.component.scss']
})
export class ConfigFasesComponent implements OnInit {

  fases: FaseDTO[] = [];
  isLoading = false;
  error = '';

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private estadoService: EntrevistasEstadoService
  ) {}

  ngOnInit(): void {
    this.cargarFases();
  }

  cargarFases(): void {
    this.isLoading = true;
    this.error = '';
    this.http.get<FaseDTO[]>('http://localhost:8080/api/evaluacion/fases').subscribe({
      next: (data) => { this.fases = data; this.isLoading = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'Error al cargar fases.'; this.isLoading = false; this.cdr.detectChanges(); }
    });
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
    const id = this.estadoService.getIdSolicitud();
    if (id) this.router.navigate(['/evaluador/entrevistas-docentes/postulantes', id]);
    else    this.router.navigate(['/evaluador/entrevistas-docentes/postulantes']);
  }

  esRutaActiva(segmento: string): boolean {
    return this.router.url.includes(segmento);
  }
}
