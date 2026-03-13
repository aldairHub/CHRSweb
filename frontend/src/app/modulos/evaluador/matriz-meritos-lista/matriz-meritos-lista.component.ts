import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

export interface ConvocatoriaLista {
  idConvocatoria: number;
  titulo: string;
  materia: string;
  fechaLimiteDocumentos: string;
  totalCandidatos: number;
  disponible: boolean;
  diasRestantes: number | null;
}

@Component({
  selector: 'app-matriz-meritos-lista',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './matriz-meritos-lista.component.html',
  styleUrls: ['./matriz-meritos-lista.component.scss']
})
export class MatrizMeritosListaComponent implements OnInit {

  private readonly API = 'http://localhost:8080/api/matriz-meritos';

  cargando = false;
  error = '';
  convocatorias: ConvocatoriaLista[] = [];

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

    this.http.get<ConvocatoriaLista[]>(`${this.API}/convocatorias`).subscribe({
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

  abrirMatriz(conv: ConvocatoriaLista): void {
    if (!conv.disponible) return;
    this.router.navigate(['/evaluador/matriz-meritos', conv.idConvocatoria]);
  }

  volver(): void {
    this.router.navigate(['/evaluador']);
  }
}
