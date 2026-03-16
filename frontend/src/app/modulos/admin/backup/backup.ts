import { Component, OnInit, OnDestroy, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../../../services/toast.service';

interface ConfigBackup {
  idConfig?: number;
  rutaPgdump: string; rutaOrigen: string; tipoBackup: string;
  retencionActiva: boolean; diasRetencion: number; numEjecuciones: number;
  horaBackup1: string; horaBackup2: string; horaBackup3: string;
  activo: boolean;
  destinoLocal: boolean;
  destinoEmail: boolean;
  rutaDestino: string;
  emailDestino: string;
  tipoDestino: string;
  notificarError: boolean; notificarExito: boolean;
}

interface HistorialBackup {
  idHistorial: number; estado: string; tipoBackup: string; origen: string;
  fechaInicio: string; tamanoFormateado: string; duracionFormateada: string;
  rutaArchivo: string; mensajeError: string;
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

  activeTab: 'config' | 'historial' | 'restaurar' = 'config';
  guardando = false; ejecutando = false; cargando = true;

  config: ConfigBackup = {
    rutaPgdump: 'C:\\Program Files\\PostgreSQL\\18\\bin\\pg_dump.exe',
    rutaOrigen: 'C:\\Backups', tipoBackup: 'COMPLETO',
    retencionActiva: false, diasRetencion: 7, numEjecuciones: 1,
    horaBackup1: '08:00', horaBackup2: '', horaBackup3: '',
    activo: false,
    destinoLocal: false, destinoEmail: false,
    rutaDestino: '', emailDestino: '',
    tipoDestino: 'NINGUNO',
    notificarError: true, notificarExito: true
  };

  emailChips: string[] = []; emailInput = ''; emailInputError = '';

  syncEmailsFromConfig() {
    this.emailChips = this.config.emailDestino?.trim()
      ? this.config.emailDestino.split(/[,;]/).map(e=>e.trim()).filter(e=>e) : [];
    this.emailInput = ''; this.emailInputError = '';
  }
  syncEmailsToConfig() { this.config.emailDestino = this.emailChips.join(', '); }
  agregarEmailChip() {
    const email = this.emailInput.trim();
    if (!email) return;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { this.emailInputError = 'Email inválido'; return; }
    if (this.emailChips.includes(email)) { this.emailInputError = 'Ya fue agregado'; return; }
    this.emailChips.push(email); this.emailInput = ''; this.emailInputError = '';
    this.syncEmailsToConfig();
    this.cdr.detectChanges();
  }
  quitarEmailChip(i: number) { this.emailChips.splice(i,1); this.syncEmailsToConfig(); this.cdr.detectChanges(); }
  onEmailKeydown(e: KeyboardEvent) {
    if (e.key==='Enter') { e.preventDefault(); this.agregarEmailChip(); }
    if (e.key==='Backspace' && !this.emailInput && this.emailChips.length>0) { this.emailChips.pop(); this.syncEmailsToConfig(); this.cdr.detectChanges(); }
  }
  trackByIndex(i: number) { return i; }

  toggleDestinoLocal() { this.config.destinoLocal = !this.config.destinoLocal; this.cdr.detectChanges(); }
  toggleDestinoEmail() { this.config.destinoEmail = !this.config.destinoEmail; this.cdr.detectChanges(); }

  historial: HistorialBackup[] = [];

  // ── Restauración ─────────────────────────────────────────────
  restaurarFile: File | null = null;
  restaurarNombre = '';
  restaurarEstado: 'idle'|'validando'|'valido'|'invalido'|'restaurando'|'ok'|'error' = 'idle';
  restaurarMensaje = '';
  restaurarConfirmando = false;

  onArchivoSeleccionado(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.restaurarFile = file; this.restaurarNombre = file?.name ?? '';
    this.restaurarEstado = 'idle'; this.restaurarMensaje = ''; this.restaurarConfirmando = false;
    if (file) this.validarArchivo();
    this.cdr.detectChanges();
  }
  onDrop(event: DragEvent) {
    event.preventDefault();
    const file = event.dataTransfer?.files?.[0] ?? null;
    if (!file) return;
    this.restaurarFile = file; this.restaurarNombre = file.name;
    this.restaurarEstado = 'idle'; this.restaurarMensaje = ''; this.restaurarConfirmando = false;
    this.validarArchivo();
    this.cdr.detectChanges();
  }
  onDragOver(e: DragEvent) { e.preventDefault(); }
  limpiarArchivo() {
    this.restaurarFile=null; this.restaurarNombre=''; this.restaurarEstado='idle';
    this.restaurarMensaje=''; this.restaurarConfirmando=false;
    this.cdr.detectChanges();
  }

  validarArchivo() {
    if (!this.restaurarFile) return;
    this.restaurarEstado = 'validando'; this.restaurarMensaje = '';
    this.cdr.detectChanges();
    const form = new FormData(); form.append('archivo', this.restaurarFile);
    this.http.post<any>('/api/backup/validar', form).subscribe({
      next: (res) => { this.restaurarEstado = res.estado==='OK' ? 'valido' : 'invalido'; this.restaurarMensaje = res.mensaje; this.cdr.detectChanges(); },
      error: (err) => { this.restaurarEstado = 'invalido'; this.restaurarMensaje = err?.error?.mensaje ?? 'No se pudo validar el archivo.'; this.cdr.detectChanges(); }
    });
  }
  confirmarRestauracion() { this.restaurarConfirmando = true; this.cdr.detectChanges(); }
  cancelarRestauracion() { this.restaurarConfirmando = false; this.cdr.detectChanges(); }
  ejecutarRestauracion() {
    if (!this.restaurarFile) return;
    this.restaurarEstado = 'restaurando'; this.restaurarConfirmando = false;
    this.cdr.detectChanges();
    const form = new FormData(); form.append('archivo', this.restaurarFile);
    this.http.post<any>('/api/backup/restaurar', form).subscribe({
      next: (res) => { this.restaurarEstado='ok'; this.restaurarMensaje=res.mensaje; this.toast.success('Base de datos restaurada correctamente.'); this.cdr.detectChanges(); },
      error: (err) => { this.restaurarEstado='error'; this.restaurarMensaje=err?.error?.mensaje ?? 'Error durante la restauración.'; this.toast.error('Error al restaurar: '+this.restaurarMensaje); this.cdr.detectChanges(); }
    });
  }

  constructor(private http: HttpClient, private toast: ToastService, private cdr: ChangeDetectorRef, private ngZone: NgZone) {}

  ngOnInit() {
    this.cargarConfig(); this.cargarHistorial();
    this.pollingInterval = setInterval(() => { this.cargarHistorial(); }, 30000);
  }
  ngOnDestroy() { if (this.pollingInterval) clearInterval(this.pollingInterval); }

  cargarConfig() {
    this.cargando = true;
    this.cdr.detectChanges();
    this.http.get<ConfigBackup>('/api/backup/config').subscribe({
      next: (cfg) => {
        this.config = cfg;
        if (cfg.destinoLocal === undefined || cfg.destinoLocal === null) {
          this.config.destinoLocal = cfg.tipoDestino === 'LOCAL';
          this.config.destinoEmail = cfg.tipoDestino === 'EMAIL';
        }
        this.syncEmailsFromConfig();
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => { this.cargando=false; this.cdr.detectChanges(); }
    });
  }
  cargarHistorial() {
    this.http.get<HistorialBackup[]>('/api/backup/historial').subscribe({
      next: (h) => { this.historial=h; this.cdr.detectChanges(); },
      error: () => {}
    });
  }
  guardarConfig() {
    this.syncEmailsToConfig(); this.guardando=true;
    this.cdr.detectChanges();
    this.http.put<ConfigBackup>('/api/backup/config', this.config).subscribe({
      next: (cfg) => {
        this.config=cfg;
        if (cfg.destinoLocal === undefined || cfg.destinoLocal === null) {
          this.config.destinoLocal = cfg.tipoDestino === 'LOCAL';
          this.config.destinoEmail = cfg.tipoDestino === 'EMAIL';
        }
        this.syncEmailsFromConfig(); this.guardando=false;
        this.cargarHistorial(); this.toast.success('Configuración guardada');
        this.cdr.detectChanges();
      },
      error: () => { this.guardando=false; this.toast.error('Error al guardar'); this.cdr.detectChanges(); }
    });
  }
  ejecutarManual() {
    this.ejecutando=true;
    this.cdr.detectChanges();
    this.http.post<any>('/api/backup/ejecutar',{}).subscribe({
      next: (res) => {
        this.ngZone.run(() => {
          this.ejecutando=false;
          res.estado==='EXITOSO' ? this.toast.success('Backup ejecutado correctamente') : this.toast.error('Backup falló: '+res.mensajeError);
          this.cargarHistorial(); this.activeTab='historial';
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.ejecutando=false;
          this.toast.error('Error al ejecutar backup');
          this.cdr.detectChanges();
        });
      }
    });
  }
  get horasConfiguradas(): string[] {
    const h=[this.config.horaBackup1];
    if(this.config.numEjecuciones>=2) h.push(this.config.horaBackup2||'--:--');
    if(this.config.numEjecuciones>=3) h.push(this.config.horaBackup3||'--:--');
    return h.filter(x=>x&&x!=='--:--');
  }
  get statusLabel(): string {
    return !this.config.activo ? 'Respaldo automático INACTIVO'
      : `Respaldo automático ACTIVO — Horarios: ${this.horasConfiguradas.join(', ')}`;
  }
}
