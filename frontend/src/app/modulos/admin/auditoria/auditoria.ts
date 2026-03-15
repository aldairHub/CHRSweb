// src/app/modulos/admin/auditoria/auditoria.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { ToastComponent } from '../../../component/toast.component';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [CommonModule, RouterLink, ToastComponent],
  templateUrl: './auditoria.html',
  styleUrls: ['./auditoria.scss']
})
export class AuditoriaComponent implements OnInit {

  private apiUrl = 'http://localhost:8080/api/admin/auditoria';

  totalHoy    = 0;
  exitososHoy = 0;
  fallidosHoy = 0;

  cambiosTotalHoy = 0;
  insertsHoy      = 0;
  updatesHoy      = 0;
  deletesHoy      = 0;

  constructor(
    private http:  HttpClient,
    private cdr:   ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.cargarResumenHoy();
    this.cargarResumenCambiosHoy();
  }

  cargarResumenHoy(): void {
    const hoy = new Date().toISOString().slice(0, 10);
    this.http.get<any>(`${this.apiUrl}/estadisticas/login`).subscribe({
      next: (data) => {
        const tendencia = this.parseJson(data.tendenciaDiaria);
        const fila = tendencia.find((r: any) => r.dia === hoy);
        if (fila) {
          this.totalHoy    = Number(fila.total);
          this.exitososHoy = Number(fila.exitosos);
          this.fallidosHoy = Number(fila.fallidos);
        }
        this.cdr.detectChanges();
      },
      error: () => {} // silencioso — es solo el resumen
    });
  }

  private parseJson(val: any): any[] {
    if (!val) return [];
    if (Array.isArray(val)) return val;
    if (typeof val === 'string') { try { return JSON.parse(val); } catch { return []; } }
    return [];
  }

  cargarResumenCambiosHoy(): void {
    const hoy = new Date().toISOString().slice(0, 10);
    this.http.get<any>(`${this.apiUrl}/estadisticas/cambios`).subscribe({
      next: (data) => {
        const tendencia = this.parseJson(data.tendenciaDiaria);
        const fila = tendencia.find((r: any) => r.dia === hoy);
        if (fila) {
          this.cambiosTotalHoy = Number(fila.total   ?? 0);
          this.insertsHoy      = Number(fila.inserts ?? 0);
          this.updatesHoy      = Number(fila.updates ?? 0);
          this.deletesHoy      = Number(fila.deletes ?? 0);
        }
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }
}
