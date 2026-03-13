// src/app/modulos/admin/auditoria/historial-acciones/historial-acciones.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ToastComponent } from '../../../../component/toast.component';
import { LoadingSpinnerComponent } from '../../../../component/loading-spinner.component';
import { ToastService } from '../../../../services/toast.service';
import {
  AuditoriaAccionesService,
  AudAccion,
  FiltrosAuditoria
} from '../../../../services/auditoria-acciones.service';

@Component({
  selector: 'app-historial-acciones',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, ToastComponent, LoadingSpinnerComponent],
  templateUrl: './historial-acciones.html',
  styleUrls: ['./historial-acciones.scss']
})
export class HistorialAccionesComponent implements OnInit {

  registros: AudAccion[] = [];
  totalElements = 0;
  totalPages    = 0;
  currentPage   = 0;
  pageSize      = 20;
  isLoading     = false;

  filtros: FiltrosAuditoria = {
    usuarioApp: '', usuarioBd: '', accion: '', entidad: '', desde: '', hasta: ''
  };

  readonly acciones  = ['CREAR', 'ACTUALIZAR', 'ELIMINAR', 'CAMBIAR_ESTADO', 'SUBIR_DOCUMENTO', 'GENERAR_REPORTE'];
  readonly entidades = ['convocatoria', 'usuario', 'prepostulacion', 'solicitud_docente'];

  detalle: AudAccion | null = null;

  constructor(
    private svc:   AuditoriaAccionesService,
    private cdr:   ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void { this.cargar(); }

  cargar(page = 0): void {
    this.isLoading = true;
    this.cdr.detectChanges();
    this.svc.listar(this.filtros, page, this.pageSize).subscribe({
      next: (data) => {
        this.registros     = data.content;
        this.totalElements = data.totalElements;
        this.totalPages    = data.totalPages;
        this.currentPage   = data.number;
        this.isLoading     = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.toast.error('Error al cargar', 'No se pudo obtener el historial de acciones.');
        this.cdr.detectChanges();
      }
    });
  }

  aplicarFiltros(): void { this.cargar(0); }

  limpiarFiltros(): void {
    this.filtros = { usuarioApp: '', usuarioBd: '', accion: '', entidad: '', desde: '', hasta: '' };
    this.cargar(0);
    this.toast.info('Filtros limpiados');
  }

  irPagina(p: number): void {
    if (p >= 0 && p < this.totalPages) this.cargar(p);
  }

  get pages(): number[] {
    const cur = this.currentPage;
    const range: number[] = [];
    for (let i = Math.max(0, cur - 2); i <= Math.min(this.totalPages - 1, cur + 2); i++) {
      range.push(i);
    }
    return range;
  }

  verDetalle(r: AudAccion): void  { this.detalle = r; }
  cerrarDetalle(): void           { this.detalle = null; }

  formatFecha(fecha: string): string {
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
  }

  badgeAccion(accion: string): string {
    const map: Record<string, string> = {
      CREAR: 'badge-crear', ACTUALIZAR: 'badge-actualizar', ELIMINAR: 'badge-eliminar',
      CAMBIAR_ESTADO: 'badge-estado', SUBIR_DOCUMENTO: 'badge-documento', GENERAR_REPORTE: 'badge-reporte'
    };
    return map[accion] ?? 'badge-default';
  }

  labelAccion(accion: string): string {
    const map: Record<string, string> = {
      CREAR: 'Crear', ACTUALIZAR: 'Actualizar', ELIMINAR: 'Eliminar',
      CAMBIAR_ESTADO: 'Cambiar estado', SUBIR_DOCUMENTO: 'Documento', GENERAR_REPORTE: 'Reporte'
    };
    return map[accion] ?? accion;
  }

  badgeEntidad(entidad: string): string {
    const map: Record<string, string> = {
      convocatoria: 'badge-entidad-conv', usuario: 'badge-entidad-user',
      prepostulacion: 'badge-entidad-pre', solicitud_docente: 'badge-entidad-sol'
    };
    return map[entidad?.toLowerCase()] ?? 'badge-entidad-default';
  }

  esDbDirecto(r: AudAccion): boolean { return r.usuarioApp === 'db_directo'; }

  parsearDescripcion(desc: string | null): { clave: string; valor: string }[] {
    if (!desc) return [];
    return desc.split('|').map(p => p.trim()).filter(Boolean).map(p => {
      const idx = p.indexOf(':');
      return idx === -1 ? { clave: '', valor: p } : { clave: p.slice(0, idx).trim(), valor: p.slice(idx + 1).trim() };
    });
  }
}
