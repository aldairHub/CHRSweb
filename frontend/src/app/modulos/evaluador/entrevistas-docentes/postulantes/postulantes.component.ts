// entrevistas-docentes/postulantes/postulantes.component.ts
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../../component/navbar';
import { PostulantesService } from '../../../../services/entrevistas/postulantes.service';
import {
  PostulanteResumen, PostulanteDetalle,
  FaseProcesoDetalle, HistorialAccion
} from '../../../../models/entrevistas-models';
import { debounceTime, Subject } from 'rxjs';

@Component({
  selector: 'app-postulantes',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, NavbarComponent],
  templateUrl: './postulantes.component.html',
  styleUrls: ['./postulantes.component.scss']
})
export class PostulantesComponent implements OnInit {

  postulantes: PostulanteResumen[] = [];
  postulanteFiltrados: PostulanteResumen[] = [];
  isLoading = true;
  error = '';

  searchTerm   = '';
  filterEstado = '';
  private searchSubject = new Subject<string>();

  page     = 1;
  pageSize = 10;
  Math     = Math;

  get paginados(): PostulanteResumen[] {
    const s = (this.page - 1) * this.pageSize;
    return this.postulanteFiltrados.slice(s, s + this.pageSize);
  }
  get totalPages(): number { return Math.max(1, Math.ceil(this.postulanteFiltrados.length / this.pageSize)); }
  getPageNums(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i + 1); }

  showDetalle = false;
  postulanteSeleccionado: PostulanteResumen | null = null;
  detalleCompleto: PostulanteDetalle | null = null;
  isLoadingDetalle = false;

  get fasesDetalle(): FaseProcesoDetalle[] { return this.detalleCompleto?.fases ?? []; }
  get historial(): HistorialAccion[]       { return this.detalleCompleto?.historial ?? []; }

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private postulantesService: PostulantesService
  ) {}

  ngOnInit(): void {
    this.searchSubject.pipe(debounceTime(350)).subscribe((q: string) => this.buscarBackend(q));

    this.route.params.subscribe(params => {
      if (params['id']) {
        this.cargarListado().then(() => {
          const p = this.postulantes.find(x => x.idProceso === +params['id']);
          if (p) this.verDetalle(p);
        });
      } else {
        this.cargarListado();
      }
    });
  }

  cargarListado(estado?: string): Promise<void> {
    this.isLoading = true; this.error = '';
    return new Promise(resolve => {
      this.postulantesService.listar(estado).subscribe({
        next: (data: PostulanteResumen[]) => {
          this.postulantes = data; this.postulanteFiltrados = [...data];
          this.isLoading = false; this.cdr.detectChanges(); resolve();
        },
        error: (err: unknown) => {
          console.error(err); this.error = 'No se pudieron cargar los postulantes.';
          this.isLoading = false; this.cdr.detectChanges(); resolve();
        }
      });
    });
  }

  applyFilters(): void {
    const t = this.searchTerm.trim().toLowerCase();
    if (t.length >= 3) { this.searchSubject.next(t); return; }
    this.postulanteFiltrados = this.postulantes.filter(p => {
      const matchE = !this.filterEstado || p.estadoGeneral === this.filterEstado;
      const matchS = !t || `${p.nombres} ${p.apellidos}`.toLowerCase().includes(t)
        || p.cedula.includes(t) || p.materia.toLowerCase().includes(t);
      return matchE && matchS;
    });
    this.page = 1; this.cdr.detectChanges();
  }

  onEstadoChange(): void {
    this.filterEstado ? this.cargarListado(this.filterEstado) : this.cargarListado();
  }

  private buscarBackend(q: string): void {
    this.postulantesService.listar(this.filterEstado || undefined, q).subscribe({
      next: (data: PostulanteResumen[]) => { this.postulanteFiltrados = data; this.page = 1; this.cdr.detectChanges(); }
    });
  }

  verDetalle(p: PostulanteResumen): void {
    this.postulanteSeleccionado = p;
    this.showDetalle     = true;
    this.isLoadingDetalle = true;
    this.detalleCompleto  = null;
    this.cdr.detectChanges();

    this.postulantesService.obtenerDetalle(p.idProceso).subscribe({
      next: (d: PostulanteDetalle) => { this.detalleCompleto = d; this.isLoadingDetalle = false; this.cdr.detectChanges(); },
      error: (err: unknown) => {
        console.error(err); this.isLoadingDetalle = false;
        alert('Error al cargar el detalle.'); this.cdr.detectChanges();
      }
    });
  }

  volverLista(): void {
    this.showDetalle = false; this.postulanteSeleccionado = null; this.detalleCompleto = null;
  }

  programarReunion(idProceso?: number, idFase?: number): void {
    const base = '/evaluador/entrevistas-docentes/programar-reunion';
    if (idProceso && idFase) {
      // Desde botón de fase específica: lleva idProceso e idFase
      this.router.navigate([base, idProceso, idFase]);
    } else if (idProceso) {
      // Desde botón del header: solo idProceso, sin idFase
      this.router.navigate([base, idProceso]);
    } else {
      // Sin contexto: va a programar sin parámetros
      this.router.navigate([base]);
    }
  }
  irEvaluar(idReunion: number): void {
    this.router.navigate(['/evaluador/entrevistas-docentes/evaluacion', idReunion]);
  }

  verResultados(idProceso: number): void {
    this.router.navigate(['/evaluador/entrevistas-docentes/resultados', idProceso]);
  }

  getNombreCompleto(p: PostulanteResumen): string { return `${p.nombres} ${p.apellidos}`; }

  getBadgeClass(e: string): string {
    return ({ en_proceso: 'warning', completado: 'success', rechazado: 'danger', pendiente: 'pending' } as Record<string,string>)[e] ?? 'default';
  }
  getBadgeLabel(e: string): string {
    return ({ en_proceso: 'En Proceso', completado: 'Completado', rechazado: 'Rechazado', pendiente: 'Pendiente' } as Record<string,string>)[e] ?? e;
  }
  getFaseBorderColor(e: string): string {
    return ({ completada: '#10b981', en_curso: '#00A63E', pendiente: '#d1d5db', bloqueada: '#d1d5db' } as Record<string,string>)[e] ?? '#d1d5db';
  }
  getFaseEstadoLabel(e: string): string {
    return ({ completada: 'Completada', en_curso: 'En Curso', pendiente: 'Pendiente', bloqueada: 'Bloqueada' } as Record<string,string>)[e] ?? e;
  }
  getFaseEstadoBadge(e: string): string {
    return ({ completada: 'success', en_curso: 'warning', pendiente: 'info', bloqueada: 'default' } as Record<string,string>)[e] ?? 'default';
  }
}
