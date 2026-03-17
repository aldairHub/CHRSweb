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

  // ── NUEVO: selector de convocatoria ───────────────────────
  misPostulaciones:       PostulanteInfo[] = [];
  idPostulacionSeleccion: number | null    = null;
  mostrarSelector         = false;

  constructor(
    private router: Router,
    private entrevistaSvc: EntrevistaService,
    private documentoSvc: DocumentoService
  ) {}

  ngOnInit(): void {
    const idUsuario = Number(localStorage.getItem('idUsuario'));
    if (!idUsuario) { this.router.navigate(['/login']); return; }

    // Carga el listado de postulaciones primero
    this.documentoSvc.listarMisPostulaciones(idUsuario).subscribe({
      next: lista => {
        this.misPostulaciones = lista;
        this.mostrarSelector  = lista.length > 1;
        if (lista.length > 0) this.idPostulacionSeleccion = lista[0].idPostulacion;
        this.cargarEntrevista(idUsuario);
      },
      error: () => this.cargarEntrevista(idUsuario)
    });
  }

  private cargarEntrevista(idUsuario: number): void {
    this.cargando = true;
    this.entrevistaSvc.obtenerMiEntrevista(idUsuario, this.idPostulacionSeleccion ?? undefined).subscribe({
      next: data => {
        this.entrevista = data;
        this.error = data ? null : 'No tienes ninguna entrevista programada por el momento.';
        setTimeout(() => this.cargando = false, 0);
      },
      error: () => {
        this.error = 'No tienes ninguna entrevista programada por el momento.';
        setTimeout(() => this.cargando = false, 0);
      }
    });
  }

  // ── NUEVO: usuario cambia convocatoria ────────────────────
  onConvocatoriaChange(idPostulacion: number): void {
    this.idPostulacionSeleccion = idPostulacion;
    const idUsuario = Number(localStorage.getItem('idUsuario'));
    this.cargarEntrevista(idUsuario);
  }

  get esVirtual(): boolean {
    return this.entrevista?.modalidad !== 'presencial';
  }

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
