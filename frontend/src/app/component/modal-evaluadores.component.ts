// ══════════════════════════════════════════════════════════════
// modal-evaluadores.component.ts
// Componente reutilizable para asignar evaluadores
// Usado tanto en Matriz de Méritos como en Entrevistas Docentes
// ══════════════════════════════════════════════════════════════
import { Component, Input, Output, EventEmitter, OnInit, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../services/toast.service';

export interface EvaluadorInfo {
  id_usuario: number;
  nombre_completo: string;
  usuario_app: string;
  es_dueno: boolean;
}

@Component({
  selector: 'app-modal-evaluadores',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-overlay" *ngIf="visible" (click)="cerrar()">
      <div class="modal-evaluadores" (click)="$event.stopPropagation()">

        <div class="modal-ev-header">
          <div class="modal-icon icon-blue">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
          </div>
          <div>
            <h3>Evaluadores Asignados</h3>
            <small>{{ contextLabel }}</small>
          </div>
          <button class="close-btn" (click)="cerrar()">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="modal-ev-body">

          <!-- Evaluadores actuales -->
          <div class="ev-section-label">Evaluadores actuales</div>
          <div class="ev-loading" *ngIf="cargando">
            <svg class="spinner" fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
            Cargando...
          </div>

          <div class="ev-list" *ngIf="!cargando">
            <div class="ev-item" *ngFor="let e of asignados">
              <div class="ev-avatar">{{ getInitials(e.nombre_completo) }}</div>
              <div class="ev-info">
                <span class="ev-nombre">{{ e.nombre_completo }}</span>
                <span class="ev-usuario">{{ e.usuario_app }}</span>
              </div>
              <span class="dueno-badge" *ngIf="e.es_dueno">Solicitante</span>
              <ng-container *ngIf="!e.es_dueno">
                <ng-container *ngIf="confirmandoQuitarId !== e.id_usuario">
                  <button class="btn-quitar" (click)="iniciarQuitar(e)" title="Remover evaluador">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                    </svg>
                  </button>
                </ng-container>
                <ng-container *ngIf="confirmandoQuitarId === e.id_usuario">
                  <div class="confirm-quitar">
                    <span class="confirm-quitar__label">¿Remover?</span>
                    <button class="confirm-quitar__si" (click)="confirmarQuitar(e)">Sí</button>
                    <button class="confirm-quitar__no" (click)="cancelarQuitar()">No</button>
                  </div>
                </ng-container>
              </ng-container>
            </div>
            <div class="ev-empty" *ngIf="asignados.length === 0">Sin evaluadores asignados.</div>
          </div>

          <!-- Agregar evaluadores -->
          <div class="ev-section-label" style="margin-top: 1.25rem;">Agregar evaluador</div>
          <div class="ev-disponibles" *ngIf="!cargando">
            <div class="ev-disponible-item" *ngFor="let e of disponibles" (click)="asignar(e)">
              <div class="ev-avatar ev-avatar--light">{{ getInitials(e.nombre_completo) }}</div>
              <div class="ev-info">
                <span class="ev-nombre">{{ e.nombre_completo }}</span>
                <span class="ev-usuario">{{ e.usuario_app }}</span>
              </div>
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16" class="add-icon">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
            </div>
            <div class="ev-empty" *ngIf="disponibles.length === 0">
              No hay más evaluadores disponibles.
            </div>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.45);
      display: flex; align-items: center; justify-content: center; z-index: 1100; padding: 1rem;
    }
    .modal-evaluadores {
      background: white; border-radius: 16px; padding: 0;
      max-width: 440px; width: 100%; box-shadow: 0 20px 60px rgba(0,0,0,0.15);
      max-height: 85vh; display: flex; flex-direction: column;
    }
    .modal-ev-header {
      display: flex; align-items: center; gap: 0.75rem;
      padding: 1.5rem 1.5rem 1rem;
      border-bottom: 1px solid #B9F8CF;
      h3 { font-size: 1rem; font-weight: 700; color: #016630; margin: 0 0 0.1rem; flex: 1; }
      small { font-size: 0.78rem; color: #536b50; }
    }
    .modal-ev-body { padding: 1.25rem 1.5rem; overflow-y: auto; }
    .close-btn {
      background: none; border: none; cursor: pointer; color: #9ca3af; padding: 0.25rem;
      border-radius: 6px; display: flex; align-items: center; justify-content: center;
      &:hover { background: #f3f4f6; color: #374151; }
    }
    .modal-icon {
      width: 36px; height: 36px; border-radius: 8px;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
      svg { width: 18px; height: 18px; }
      &.icon-blue { background: #dbeafe; svg { stroke: #2563eb; } }
    }
    .ev-section-label {
      font-size: 0.72rem; font-weight: 700; color: #536b50;
      text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.625rem;
    }
    .ev-list, .ev-disponibles { display: flex; flex-direction: column; gap: 0.375rem; }
    .ev-item, .ev-disponible-item {
      display: flex; align-items: center; gap: 0.75rem;
      padding: 0.625rem 0.75rem; border-radius: 8px;
      border: 1px solid #B9F8CF; background: #EEF9EC;
    }
    .ev-disponible-item {
      background: #f9fafb; border-color: #e5e7eb; cursor: pointer; transition: all 0.2s;
      &:hover { background: #EEF9EC; border-color: #B9F8CF; }
    }
    .ev-avatar {
      width: 34px; height: 34px; border-radius: 50%;
      background: #016630; color: white;
      display: flex; align-items: center; justify-content: center;
      font-size: 0.75rem; font-weight: 700; flex-shrink: 0;
      &.ev-avatar--light { background: #d1d5db; color: #374151; }
    }
    .ev-info { flex: 1; min-width: 0; }
    .ev-nombre { display: block; font-size: 0.875rem; font-weight: 600; color: #1a1a1a; }
    .ev-usuario { display: block; font-size: 0.75rem; color: #536b50; }
    .dueno-badge {
      padding: 0.15rem 0.5rem; border-radius: 20px;
      background: #dbeafe; color: #1e40af; font-size: 0.72rem; font-weight: 700;
    }
    .btn-quitar {
      background: none; border: none; cursor: pointer; color: #9ca3af; padding: 0.25rem;
      border-radius: 6px; display: flex;
      &:hover { background: #fee2e2; color: #ef4444; }
    }
    .add-icon { color: #00A63E; flex-shrink: 0; }
    .ev-empty { text-align: center; color: #9ca3af; font-size: 0.85rem; padding: 1rem 0; }
    .ev-loading {
      display: flex; align-items: center; gap: 0.5rem;
      color: #536b50; font-size: 0.85rem; padding: 1rem 0;
    }
    .spinner { animation: spin 0.8s linear infinite; }
    @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
    .confirm-quitar {
      display: flex; align-items: center; gap: 0.35rem; flex-shrink: 0;
    }
    .confirm-quitar__label {
      font-size: 0.72rem; font-weight: 700; color: #991b1b; white-space: nowrap;
    }
    .confirm-quitar__si {
      padding: 0.2rem 0.5rem; border-radius: 5px; border: none;
      background: #ef4444; color: white; font-size: 0.72rem; font-weight: 700;
      cursor: pointer; transition: background 0.15s;
      &:hover { background: #dc2626; }
    }
    .confirm-quitar__no {
      padding: 0.2rem 0.5rem; border-radius: 5px;
      border: 1px solid #e5e7eb; background: white; color: #374151;
      font-size: 0.72rem; font-weight: 600; cursor: pointer; transition: background 0.15s;
      &:hover { background: #f3f4f6; }
    }
  `]
})
export class ModalEvaluadoresComponent implements OnInit, OnChanges {

  private readonly API = 'http://localhost:8080/api/evaluadores-asignados';

  @Input() visible = false;
  @Input() idProceso: number | null = null;
  @Input() idSolicitud: number | null = null;
  @Input() tipo: 'matriz' | 'entrevista' = 'matriz';
  @Input() contextLabel = '';
  @Output() cerrado = new EventEmitter<void>();

  asignados: EvaluadorInfo[] = [];
  disponibles: EvaluadorInfo[] = [];
  cargando = false;
  confirmandoQuitarId: number | null = null;

  constructor(private http: HttpClient, private toast: ToastService) {}

  ngOnInit(): void {
    if (this.visible && this.idProceso) this.cargar();
  }

  ngOnChanges(): void {
    if (this.visible && this.idProceso) this.cargar();
  }

  // Normaliza las columnas del backend (v_nombre_completo, v_id_usuario, etc.)
  private mapEvaluador(item: any): EvaluadorInfo {
    return {
      id_usuario:      item.id_usuario      ?? item.v_id_usuario      ?? 0,
      nombre_completo: item.nombre_completo ?? item.v_nombre_completo ?? '',
      usuario_app:     item.usuario_app     ?? item.v_usuario_app     ?? '',
      es_dueno:        item.es_dueno        ?? item.v_es_dueno        ?? false
    };
  }

  cargar(): void {
    if (!this.idProceso) return;
    this.cargando = true;
    this.confirmandoQuitarId = null;

    this.http.get<any[]>(`${this.API}/${this.idProceso}?tipo=${this.tipo}`).subscribe({
      next: (data) => { this.asignados = data.map(i => this.mapEvaluador(i)); this.cargarDisponibles(); },
      error: () => { this.cargando = false; }
    });
  }

  cargarDisponibles(): void {
    const solicitudParam = this.idSolicitud ? `&idSolicitud=${this.idSolicitud}` : '';
    this.http.get<any[]>(`${this.API}/${this.idProceso}/disponibles?tipo=${this.tipo}${solicitudParam}`).subscribe({
      next: (data) => { this.disponibles = data.map(i => this.mapEvaluador(i)); this.cargando = false; },
      error: () => { this.cargando = false; }
    });
  }

  asignar(e: EvaluadorInfo): void {
    this.http.post(`${this.API}/${this.idProceso}`, {
      idUsuario: e.id_usuario, tipo: this.tipo
    }).subscribe({
      next: () => { this.toast.success('Evaluador asignado', e.nombre_completo); this.cargar(); },
      error: (err) => this.toast.error('Error al asignar', err?.error?.mensaje)
    });
  }

  iniciarQuitar(e: EvaluadorInfo): void {
    this.confirmandoQuitarId = e.id_usuario;
  }

  cancelarQuitar(): void {
    this.confirmandoQuitarId = null;
  }

  confirmarQuitar(e: EvaluadorInfo): void {
    this.http.delete(`${this.API}/${this.idProceso}/${e.id_usuario}?tipo=${this.tipo}`).subscribe({
      next: () => { this.toast.success('Evaluador removido', e.nombre_completo); this.cargar(); },
      error: (err) => { this.toast.error('Error al remover', err?.error?.mensaje); this.confirmandoQuitarId = null; }
    });
  }

  cerrar(): void { this.cerrado.emit(); }

  getInitials(nombre: string): string {
    return nombre.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase();
  }
}
