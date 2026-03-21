import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { NavbarComponent } from '../../../../component/navbar';
import { FooterComponent } from '../../../../component/footer';
import { EntrevistasEstadoService } from '../../../../services/entrevistas/entrevistas-estado.service';

export interface SolicitudEntrevista {
  idSolicitud: number;
  materia: string;
  totalCandidatos: number;
  candidatosConMatriz: number;
  disponible: boolean;
  mensajeBloqueo: string | null;
}

export interface ConvocatoriaEntrevistaAgrupada {
  idConvocatoria: number;
  titulo: string;
  solicitudes: SolicitudEntrevista[];
  expandida: boolean;
  tieneMultiples: boolean;
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
  convocatorias: ConvocatoriaEntrevistaAgrupada[] = [];

  constructor(
    private router: Router,
    private http: HttpClient,
    private estado: EntrevistasEstadoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.estado.limpiar();
    this.cargarConvocatorias();
  }

  cargarConvocatorias(): void {
    this.cargando = true;
    this.error = '';
    this.cdr.detectChanges();

    this.http.get<any[]>(`${this.API}/convocatorias`).subscribe({
      next: (data) => {
        this.convocatorias = this.agrupar(data);
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error al cargar las convocatorias.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  private agrupar(rows: any[]): ConvocatoriaEntrevistaAgrupada[] {
    const map = new Map<number, ConvocatoriaEntrevistaAgrupada>();

    for (const row of rows) {
      if (!map.has(row.idConvocatoria)) {
        map.set(row.idConvocatoria, {
          idConvocatoria: row.idConvocatoria,
          titulo:         row.titulo,
          solicitudes:    [],
          expandida:      false,
          tieneMultiples: false
        });
      }

      const conv = map.get(row.idConvocatoria)!;
      conv.solicitudes.push({
        idSolicitud:         row.idSolicitud,
        materia:             row.materia,
        totalCandidatos:     row.totalCandidatos,
        candidatosConMatriz: row.candidatosConMatriz,
        disponible:          row.disponible,
        mensajeBloqueo:      row.mensajeBloqueo
      });
    }

    const result = Array.from(map.values());
    result.forEach(c => {
      c.tieneMultiples = c.solicitudes.length > 1;
      if (!c.tieneMultiples) c.expandida = true;
    });
    return result;
  }

  toggleExpandir(conv: ConvocatoriaEntrevistaAgrupada): void {
    if (conv.tieneMultiples) conv.expandida = !conv.expandida;
  }

  abrirEntrevistas(sol: SolicitudEntrevista): void {
    if (!sol.disponible) return;
    this.estado.setIdSolicitud(sol.idSolicitud);
    this.router.navigate(['/evaluador/entrevistas-docentes/dashboard']);
  }

  algunaDisponible(conv: ConvocatoriaEntrevistaAgrupada): boolean {
    return conv.solicitudes.some(s => s.disponible);
  }

  volver(): void {
    this.router.navigate(['/evaluador']);
  }
}
