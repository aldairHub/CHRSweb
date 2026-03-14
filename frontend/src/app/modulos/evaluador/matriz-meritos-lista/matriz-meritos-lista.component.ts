import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

export interface SolicitudItem {
  idSolicitud: number;
  materia: string;
  totalCandidatos: number;
  disponible: boolean;
  mensajeBloqueo: string | null;
}

export interface ConvocatoriaAgrupada {
  idConvocatoria: number;
  titulo: string;
  fechaLimiteDocumentos: string | null;
  solicitudes: SolicitudItem[];
  expandida: boolean;
  tieneMultiples: boolean;
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
  convocatorias: ConvocatoriaAgrupada[] = [];

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

    this.http.get<any[]>(`${this.API}/convocatorias`).subscribe({
      next: (data) => {
        this.convocatorias = this.agrupar(data);
        this.cargando = false;
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error al cargar las convocatorias.';
        this.cargando = false;
      }
    });
  }

  // Agrupa las filas por convocatoria
  private agrupar(rows: any[]): ConvocatoriaAgrupada[] {
    const map = new Map<number, ConvocatoriaAgrupada>();

    for (const row of rows) {
      if (!map.has(row.idConvocatoria)) {
        map.set(row.idConvocatoria, {
          idConvocatoria:        row.idConvocatoria,
          titulo:                row.titulo,
          fechaLimiteDocumentos: row.fechaLimiteDocumentos,
          solicitudes:           [],
          expandida:             false,
          tieneMultiples:        false
        });
      }

      const conv = map.get(row.idConvocatoria)!;
      conv.solicitudes.push({
        idSolicitud:    row.idSolicitud,
        materia:        row.materia,
        totalCandidatos: row.totalCandidatos,
        disponible:     row.disponible,
        mensajeBloqueo: row.mensajeBloqueo
      });
    }

    const result = Array.from(map.values());
    result.forEach(c => {
      c.tieneMultiples = c.solicitudes.length > 1;
      // Si tiene una sola solicitud, mostrarla expandida por defecto
      if (!c.tieneMultiples) c.expandida = true;
    });
    return result;
  }

  toggleExpandir(conv: ConvocatoriaAgrupada): void {
    if (conv.tieneMultiples) conv.expandida = !conv.expandida;
  }

  abrirMatriz(sol: SolicitudItem): void {
    if (!sol.disponible) return;
    this.router.navigate(['/evaluador/matriz-meritos', sol.idSolicitud]);
  }

  // ¿Alguna solicitud de la convocatoria está disponible?
  algunaDisponible(conv: ConvocatoriaAgrupada): boolean {
    return conv.solicitudes.some(s => s.disponible);
  }

  volver(): void {
    this.router.navigate(['/evaluador']);
  }
}
