import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ToastComponent } from '../../../../component/toast.component';
import { ToastService } from '../../../../services/toast.service';

// ── Interfaces ───────────────────────────────────────────────────────────────

interface IntegridadRegistro {
  idAudCambio:    number;
  fecha:          string;
  tabla:          string;
  operacion:      string;
  campo:          string;
  usuarioApp:     string;
  hashGuardado:   string;
  hashCalculado:  string;
  estado:         'OK' | 'ALTERADO' | 'SIN_HASH';
}

interface IntegridadResumen {
  totalVerificados:      number;
  totalOk:               number;
  totalAlterados:        number;
  totalSinHash:          number;
  registrosSospechosos:  IntegridadRegistro[];
}

@Component({
  selector: 'app-integridad-auditoria',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, ToastComponent],
  templateUrl: './integridad.html',
  styleUrls: ['./integridad.scss']
})
export class IntegridadAuditoriaComponent implements OnInit {

  resumen:  IntegridadResumen | null = null;
  loading   = false;
  limite    = 1000;

  constructor(
    private http:   HttpClient,
    private toast:  ToastService,
    private cdr:    ChangeDetectorRef
  ) {}

  ngOnInit(): void {}

  verificar(): void {
    this.loading = true;
    this.resumen = null;
    this.cdr.detectChanges();

    const params = new HttpParams().set('limite', this.limite.toString());

    this.http.get<IntegridadResumen>(
      '/api/admin/auditoria/integridad/verificar', { params }
    ).subscribe({
      next: data => {
        this.resumen = data;
        this.loading = false;
        this.cdr.detectChanges();

        if (data.totalAlterados > 0) {
          this.toast.error('Alerta de integridad',
            `${data.totalAlterados} registro(s) ALTERADO(s) detectado(s)`);
        } else if (data.totalSinHash > 0) {
          this.toast.warning('Registros sin hash',
            `${data.totalSinHash} registro(s) sin hash (anteriores a la migración)`);
        } else {
          this.toast.success('Verificación completa',
            'Todos los registros son correctos');
        }
      },
      error: err => {
        this.loading = false;
        this.cdr.detectChanges();
        this.toast.error('Error', 'Error al verificar integridad: ' + (err.error?.message || err.message));
      }
    });
  }

  get todoOk(): boolean {
    return !!this.resumen &&
      this.resumen.totalAlterados === 0 &&
      this.resumen.totalSinHash   === 0;
  }

  get porcentajeOk(): number {
    if (!this.resumen || this.resumen.totalVerificados === 0) return 0;
    return Math.round((this.resumen.totalOk / this.resumen.totalVerificados) * 100);
  }

  estadoBadgeClass(estado: string): string {
    switch (estado) {
      case 'OK':       return 'badge-ok';
      case 'ALTERADO': return 'badge-alterado';
      case 'SIN_HASH': return 'badge-sin-hash';
      default:         return '';
    }
  }

  formatFecha(fecha: string): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  truncateHash(hash: string | null): string {
    if (!hash) return '—';
    return hash.substring(0, 8) + '…';
  }
}
