import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

export interface ConvocatoriaEntrevista {
  idConvocatoria: number;
  titulo: string;
  materia: string;
  fechaLimiteDocumentos: string;
  totalCandidatos: number;
  candidatosConMatriz: number;   // cuántos ya tienen puntaje_matriz > 0
  disponible: boolean;           // true si al menos 1 candidato tiene matriz calificada
  mensajeBloqueo: string | null;
}

@Component({
  selector: 'app-entrevistas-lista',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './entrevistas-lista.component.html',
  styleUrls: ['./entrevistas-lista.component.scss']
})
export class EntrevistasListaComponent implements OnInit {

  private readonly API = 'http://localhost:8080/api/evaluacion/procesos';

  cargando = false;
  error = '';
  convocatorias: ConvocatoriaEntrevista[] = [];

  constructor(
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.cargarConvocatorias();
  }

  cargarConvocatorias(): void {
    this.cargando = true;
    this.error = '';

    this.http.get<ConvocatoriaEntrevista[]>(`${this.API}/convocatorias`).subscribe({
      next: (data) => {
        this.convocatorias = data;
        this.cargando = false;
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error al cargar las convocatorias.';
        this.cargando = false;
      }
    });
  }

  abrirEntrevistas(conv: ConvocatoriaEntrevista): void {
    if (!conv.disponible) return;
    this.router.navigate(['/evaluador/entrevistas-docentes/postulantes', conv.idConvocatoria]);
  }

  volver(): void {
    this.router.navigate(['/evaluador']);
  }
}
