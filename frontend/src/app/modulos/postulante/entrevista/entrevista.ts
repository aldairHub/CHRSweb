import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EntrevistaService, EntrevistaInfo } from '../../../services/entrevista.service';
import { DocumentoService, PostulanteInfo } from '../../../services/documento.service';

@Component({
  selector: 'app-entrevista-postulante',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './entrevista.html',
  styleUrls: ['./entrevista.scss']
})
export class EntrevistaPostulanteComponent implements OnInit {

  mostrarModalVideoconf = false;
  cargando = true;
  error: string | null = null;

  entrevista: EntrevistaInfo | null = null;
  postulante = { nombre: '', proceso: '' };

  // Selector de convocatoria
  misPostulaciones:       PostulanteInfo[] = [];
  idPostulacionSeleccion: number | null    = null;

  // Siempre visible, bloqueado si solo hay 1
  get mostrarSelector(): boolean { return this.misPostulaciones.length >= 1; }
  get selectorBloqueado(): boolean { return this.misPostulaciones.length <= 1; }

  constructor(
    private router: Router,
    private entrevistaSvc: EntrevistaService,
    private documentoSvc: DocumentoService
  ) {}

  ngOnInit(): void {
    const idUsuario = Number(localStorage.getItem('idUsuario'));
    if (!idUsuario) { this.router.navigate(['/login']); return; }

    this.documentoSvc.listarMisPostulaciones(idUsuario).subscribe({
      next: lista => {
        this.misPostulaciones       = lista ?? [];
        this.idPostulacionSeleccion = this.misPostulaciones.length > 0
          ? this.misPostulaciones[0].idPostulacion
          : null;
        this.cargarEntrevista(idUsuario);
      },
      // Si falla la carga de postulaciones, igual intentamos cargar la entrevista
      error: () => {
        this.misPostulaciones       = [];
        this.idPostulacionSeleccion = null;
        this.cargarEntrevista(idUsuario);
      }
    });
  }

  private cargarEntrevista(idUsuario: number): void {
    this.cargando  = true;
    this.entrevista = null;
    this.error     = null;

    this.entrevistaSvc
      .obtenerMiEntrevista(idUsuario, this.idPostulacionSeleccion ?? undefined)
      .subscribe({
        next: (data: EntrevistaInfo | null) => {
          // El backend puede devolver 204 (sin body) → Angular pasa null como data
          if (data) {
            this.entrevista = data;
            this.error      = null;
          } else {
            this.entrevista = null;
            this.error      = 'No tienes ninguna entrevista programada por el momento.';
          }
          this.cargando = false;
        },
        error: () => {
          this.entrevista = null;
          this.error      = 'No tienes ninguna entrevista programada por el momento.';
          this.cargando   = false;
        }
      });
  }

  onConvocatoriaChange(idPostulacion: number): void {
    if (this.selectorBloqueado) return;
    const id = Number(idPostulacion);
    if (!id) return;
    this.idPostulacionSeleccion = id;
    const idUsuario = Number(localStorage.getItem('idUsuario'));
    if (!idUsuario) return;
    this.cargarEntrevista(idUsuario);
  }

  get esVirtual(): boolean { return this.entrevista?.modalidad !== 'presencial'; }

  get colorEstado(): string {
    if (this.entrevista?.estado === 'en_curso')   return 'en-curso';
    if (this.entrevista?.estado === 'completada') return 'completada';
    return 'programada';
  }

  abrirModalVideoconf(): void { this.mostrarModalVideoconf = true; }

  confirmarUnirse(): void {
    this.mostrarModalVideoconf = false;
    if (this.entrevista?.enlace) window.open(this.entrevista.enlace, '_blank');
  }

  volver(): void { this.router.navigate(['/postulante']); }
}
