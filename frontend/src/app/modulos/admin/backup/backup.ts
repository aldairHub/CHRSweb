import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../../../services/toast.service';

interface ConfigBackup {
  idConfig?: number;
  rutaPgdump: string;
  rutaOrigen: string;
  tipoBackup: string;
  retencionActiva: boolean;
  diasRetencion: number;
  numEjecuciones: number;
  horaBackup1: string;
  horaBackup2: string;
  horaBackup3: string;
  activo: boolean;
  tipoDestino: string;
  rutaDestino: string;
  emailDestino: string;
  notificarError: boolean;
  notificarExito: boolean;
}

interface HistorialBackup {
  idHistorial: number;
  estado: string;
  tipoBackup: string;
  origen: string;
  fechaInicio: string;
  tamanoFormateado: string;
  duracionFormateada: string;
  rutaArchivo: string;
  mensajeError: string;
}

@Component({
  selector: 'app-backup',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './backup.html',
  styleUrls: ['./backup.scss']
})
export class BackupComponent implements OnInit, OnDestroy {
  private pollingInterval: any;

  activeTab: 'config' | 'historial' = 'config';
  guardando = false;
  ejecutando = false;
  cargando = true;

  config: ConfigBackup = {
    rutaPgdump: 'C:\\Program Files\\PostgreSQL\\18\\bin\\pg_dump.exe',
    rutaOrigen: 'C:\\Backups',
    tipoBackup: 'COMPLETO',
    retencionActiva: false,
    diasRetencion: 7,
    numEjecuciones: 2,
    horaBackup1: '08:00',
    horaBackup2: '20:00',
    horaBackup3: '',
    activo: true,
    tipoDestino: 'EMAIL',
    rutaDestino: '',
    emailDestino: '',
    notificarError: true,
    notificarExito: false
  };

  // Gestión de múltiples emails como chips
  emailChips: string[] = [];
  emailInput = '';
  emailInputError = '';

  syncEmailsFromConfig() {
    if (this.config.emailDestino && this.config.emailDestino.trim()) {
      this.emailChips = this.config.emailDestino
        .split(/[,;]/)
        .map(e => e.trim())
        .filter(e => e.length > 0);
    } else {
      this.emailChips = [];
    }
    this.emailInput = '';
    this.emailInputError = '';
  }

  syncEmailsToConfig() {
    this.config.emailDestino = this.emailChips.join(', ');
  }

  agregarEmailChip() {
    const email = this.emailInput.trim();
    if (!email) return;
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      this.emailInputError = 'Email inválido';
      return;
    }
    if (this.emailChips.includes(email)) {
      this.emailInputError = 'Este email ya fue agregado';
      return;
    }
    this.emailChips.push(email);
    this.emailInput = '';
    this.emailInputError = '';
    this.syncEmailsToConfig();
  }

  quitarEmailChip(i: number) {
    this.emailChips.splice(i, 1);
    this.syncEmailsToConfig();
  }

  onEmailKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.agregarEmailChip();
    }
    if (event.key === 'Backspace' && !this.emailInput && this.emailChips.length > 0) {
      this.emailChips.pop();
      this.syncEmailsToConfig();
    }
  }

  trackByIndex(index: number) { return index; }

  toggleDestino(tipo: string) {
    // Si ya está seleccionado ese tipo, desactivar (NINGUNO)
    if (this.config.tipoDestino === tipo) {
      this.config.tipoDestino = 'NINGUNO';
    } else {
      this.config.tipoDestino = tipo;
    }
  }

  historial: HistorialBackup[] = [];

  constructor(private http: HttpClient, private toast: ToastService) {}

  ngOnDestroy(): void {
        throw new Error("Method not implemented.");
    }

  ngOnInit() {
    this.cargarConfig();
    this.cargarHistorial();
  }

  cargarConfig() {
    this.cargando = true;
    this.http.get<ConfigBackup>('/api/backup/config').subscribe({
      next: (cfg) => {
        this.config = cfg;
        this.syncEmailsFromConfig();
        this.cargando = false;
      },
      error: () => { this.cargando = false; }
    });
  }

  cargarHistorial() {
    this.http.get<HistorialBackup[]>('/api/backup/historial').subscribe({
      next: (h) => { this.historial = h; },
      error: () => {}
    });
  }

  guardarConfig() {
    this.syncEmailsToConfig();
    this.guardando = true;
    this.http.put<ConfigBackup>('/api/backup/config', this.config).subscribe({
      next: (cfg) => {
        this.config = cfg;
        this.syncEmailsFromConfig();
        this.guardando = false;
        this.toast.success('Configuración guardada');
      },
      error: () => {
        this.guardando = false;
        this.toast.error('Error al guardar configuración');
      }
    });
  }

  ejecutarManual() {
    this.ejecutando = true;
    this.http.post<any>('/api/backup/ejecutar', {}).subscribe({
      next: (res) => {
        this.ejecutando = false;
        if (res.estado === 'EXITOSO') {
          this.toast.success('Backup ejecutado correctamente');
        } else {
          this.toast.error('Backup falló: ' + res.mensajeError);
        }
        this.cargarHistorial();
        this.activeTab = 'historial';
      },
      error: () => {
        this.ejecutando = false;
        this.toast.error('Error al ejecutar backup');
      }
    });
  }

  get horasConfiguradas(): string[] {
    const horas = [this.config.horaBackup1];
    if (this.config.numEjecuciones >= 2) horas.push(this.config.horaBackup2 || '--:--');
    if (this.config.numEjecuciones >= 3) horas.push(this.config.horaBackup3 || '--:--');
    return horas.filter(h => h && h !== '--:--');
  }

  get statusLabel(): string {
    if (!this.config.activo) return 'Respaldo automático INACTIVO';
    const horas = this.horasConfiguradas;
    return `Respaldo automático ACTIVO — Horarios programados: ${horas.join(', ')}`;
  }
}
