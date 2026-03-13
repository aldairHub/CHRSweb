import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { EntrevistaService, EntrevistaInfo } from '../../../services/entrevista.service';

@Component({
  selector: 'app-entrevista-postulante',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './entrevista.html',
  styleUrls: ['./entrevista.scss']
})
export class EntrevistaPostulanteComponent implements OnInit {

  mostrarModalVideoconf = false;
  cargando = true;
  error: string | null = null;

  entrevista: EntrevistaInfo | null = null;

  postulante = {
    nombre: '',
    proceso: ''
  };

  constructor(
    private router: Router,
    private entrevistaSvc: EntrevistaService
  ) {}

  ngOnInit(): void {
    const idUsuario = Number(localStorage.getItem('idUsuario'));
    if (!idUsuario) { this.router.navigate(['/login']); return; }

    this.entrevistaSvc.obtenerMiEntrevista(idUsuario).subscribe({
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

  get esVirtual(): boolean {
    return this.entrevista?.modalidad !== 'presencial';
  }

  get colorEstado(): string {
    if (this.entrevista?.estado === 'en_curso')   return 'en-curso';
    if (this.entrevista?.estado === 'completada') return 'completada';
    return 'programada';
  }

  abrirModalVideoconf(): void {
    this.mostrarModalVideoconf = true;
  }

  confirmarUnirse(): void {
    this.mostrarModalVideoconf = false;
    if (this.entrevista?.enlace) {
      window.open(this.entrevista.enlace, '_blank');
    }
  }

  volver(): void {
    this.router.navigate(['/postulante']);
  }
}
