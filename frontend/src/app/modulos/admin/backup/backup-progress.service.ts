import { Injectable } from '@angular/core';

export type BkEstado  = 'idle' | 'ejecutando' | 'ok' | 'error';
export type BkOrigen  = 'manual' | 'automatico';

@Injectable({ providedIn: 'root' })
export class BackupProgressService {
  estado:   BkEstado = 'idle';
  progreso: number   = 0;
  fase:     string   = '';
  origen:   BkOrigen = 'manual';

  get activo(): boolean { return this.estado !== 'idle'; }
  get ejecutando(): boolean { return this.estado === 'ejecutando'; }

  reset(): void {
    this.estado = 'idle'; this.progreso = 0; this.fase = ''; this.origen = 'manual';
  }
}
