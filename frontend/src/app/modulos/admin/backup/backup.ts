import { Component, OnInit, OnDestroy,ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../../../services/toast.service';

// ── Interfaces ────────────────────────────────────────────────────
interface ConfigBackup {
  idConfig?: number;
  rutaPgdump: string; rutaOrigen: string; tipoBackup: string;
  retencionActiva: boolean; diasRetencion: number; numEjecuciones: number;
  horaBackup1: string; horaBackup2: string; horaBackup3: string;
  activo: boolean;
  destinoLocal: boolean; destinoEmail: boolean; destinoDrive: boolean;
  rutaDestino: string; emailDestino: string;
  driveFolderName: string; driveFolderId: string;
  tipoDestino: string;
  notificarError: boolean; notificarExito: boolean;
}

interface HistorialBackup {
  idHistorial: number; estado: string; tipoBackup: string;
  tipoBackupExt: string; origen: string;
  fechaInicio: string; tamanoFormateado: string; duracionFormateada: string;
  rutaArchivo: string; mensajeError: string;
  driveSubido: boolean; emailEnviado: boolean; driveUrl: string;
}

interface DriveConfigResponse {
  id: number | null;
  clientIdPreview: string | null;
  credencialesGuardadas: boolean;
  autorizado: boolean;
  redirectUri: string | null;
  folderName: string | null;
  folderId: string | null;
  expiresAt: string | null;
  updatedAt: string | null;
  authUrl: string | null;
}

interface DriveConfigForm {
  clientId: string;
  clientSecret: string;
  redirectUri: string;
  folderName: string;
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
  private authCheckInterval: any;
  private authMessageHandler: (e: MessageEvent) => void;

  activeTab: 'config' | 'historial' | 'drive' | 'restaurar' = 'config';
  guardando        = false;
  cargando         = true;
  ejecutandoTipo: string | null = null;

  // ── Config backup ──────────────────────────────────────────────
  config: ConfigBackup = {
    rutaPgdump: 'pg_dump', rutaOrigen: 'C:\\Backups', tipoBackup: 'FULL',
    retencionActiva: false, diasRetencion: 7, numEjecuciones: 1,
    horaBackup1: '08:00', horaBackup2: '', horaBackup3: '',
    activo: false, destinoLocal: false, destinoEmail: false, destinoDrive: false,
    rutaDestino: '', emailDestino: '', driveFolderName: 'SSDC-Backups',
    driveFolderId: '', tipoDestino: 'NINGUNO',
    notificarError: true, notificarExito: true
  };

  // ── Historial ──────────────────────────────────────────────────
  historial: HistorialBackup[] = [];

  // ── Drive ──────────────────────────────────────────────────────
  driveConfig: DriveConfigResponse = {
    id: null, clientIdPreview: null,
    credencialesGuardadas: false, autorizado: false,
    redirectUri: null, folderName: null, folderId: null,
    expiresAt: null, updatedAt: null, authUrl: null
  };

  driveForm: DriveConfigForm = {
    clientId: '', clientSecret: '', redirectUri: '', folderName: 'SSDC-Backups'
  };

  driveStep: 'creds' | 'auth' | 'done' = 'creds';
  driveGuardando   = false;
  driveAutorizando = false;
  mostrarSecret    = false;

  // ── Email chips ────────────────────────────────────────────────
  emailChips: string[] = [];
  emailInput      = '';
  emailInputError = '';

  // ── Restaurar ──────────────────────────────────────────────────
  restaurarFile: File | null = null;
  restaurarNombre = '';
  restaurarEstado: 'idle'|'validando'|'valido'|'invalido'|'restaurando'|'ok'|'error' = 'idle';
  restaurarMensaje    = '';
  restaurarConfirmando = false;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef,private toast: ToastService) {
    // Bind handler once so we can remove it properly on destroy
    this.authMessageHandler = (event: MessageEvent) => {
      if (event.data === 'drive_auth_ok') {
        this.toast.success('Google Drive autorizado correctamente');
        this.cargarDriveConfig();
      }
    };
  }

  ngOnInit(): void {
    this.cargarConfig();
    this.cargarHistorial();
    this.cargarDriveConfig();
    this.cdr.detectChanges();
    // Polling cada 15s para actualizar historial automáticamente
    this.pollingInterval = setInterval(() => this.cargarHistorial(), 15_000);
    window.addEventListener('message', this.authMessageHandler);
  }

  ngOnDestroy(): void {
    clearInterval(this.pollingInterval);
    clearInterval(this.authCheckInterval);
    window.removeEventListener('message', this.authMessageHandler);
  }

  // ═══════════════════════════════════════════════════════════════
  // CONFIG
  // ═══════════════════════════════════════════════════════════════
  cargarConfig(): void {
    this.cargando = true;
    this.http.get<ConfigBackup>('/api/backup/config').subscribe({
      next: cfg => {
        this.config = { ...this.config, ...cfg };
        if (this.config.destinoDrive == null) this.config.destinoDrive = false;
        this.syncEmailsFromConfig();
        this.cargando = false;this.cdr.detectChanges();
      },
      error: () => { this.cargando = false; }
    });
  }

  guardarConfig(): void {
    this.syncEmailsToConfig();
    this.guardando = true;
    this.http.put<ConfigBackup>('/api/backup/config', this.config).subscribe({
      next: cfg => {
        this.config = { ...this.config, ...cfg };
        this.syncEmailsFromConfig();
        this.guardando = false;
        this.cargarHistorial();
        this.toast.success('Configuración guardada');this.cdr.detectChanges();
      },
      error: () => { this.guardando = false; this.toast.error('Error al guardar'); }
    });
  }

  // ═══════════════════════════════════════════════════════════════
  // EJECUTAR
  // ═══════════════════════════════════════════════════════════════
  ejecutarTipo(tipo: string): void {
    if (this.ejecutandoTipo) return;
    this.ejecutandoTipo = tipo;
    this.http.post<any>(`/api/backup/ejecutar/${tipo}`, {}).subscribe({
      next: res => {
        this.ejecutandoTipo = null;
        res.estado === 'EXITOSO'
          ? this.toast.success(`Backup ${tipo} completado — ${res.tamanoFormateado}`)
          : this.toast.error(`Backup ${tipo} falló: ${res.mensajeError}`);
        this.cargarHistorial();
        this.activeTab = 'historial';
      },
      error: err => {
        this.ejecutandoTipo = null;
        this.toast.error(err?.error?.error ?? `Error al ejecutar ${tipo}`);
      }
    });
  }

  ejecutarManual(): void { this.ejecutarTipo('FULL'); }
  estaEjecutando(tipo: string): boolean { return this.ejecutandoTipo === tipo; }

  // ═══════════════════════════════════════════════════════════════
  // HISTORIAL
  // ═══════════════════════════════════════════════════════════════
  cargarHistorial(): void {
    this.http.get<HistorialBackup[]>('/api/backup/historial').subscribe({
      next: h => { this.historial = h; },
      error: () => {}
    });this.cdr.detectChanges();
  }

  get estadisticas() {
    const total    = this.historial.length;
    const exitosos = this.historial.filter(h => h.estado === 'EXITOSO').length;
    return { total, exitosos, fallidos: total - exitosos };this.cdr.detectChanges();
  }

  // ═══════════════════════════════════════════════════════════════
  // GOOGLE DRIVE
  // ═══════════════════════════════════════════════════════════════
  cargarDriveConfig(): void {
    this.http.get<DriveConfigResponse>('/api/backup/drive/config').subscribe({
      next: cfg => {
        this.driveConfig = cfg;
        this.sincronizarDriveStep(cfg);
        if (cfg.redirectUri) this.driveForm.redirectUri = cfg.redirectUri;
        if (cfg.folderName)  this.driveForm.folderName  = cfg.folderName;
      },
      error: () => {}
    });this.cdr.detectChanges();
  }

  private sincronizarDriveStep(cfg: DriveConfigResponse): void {
    if (cfg.autorizado)              this.driveStep = 'done';
    else if (cfg.credencialesGuardadas) this.driveStep = 'auth';
    else                             this.driveStep = 'creds';
  }

  guardarCredencialesDrive(): void {
    if (!this.driveForm.clientId.trim()) { this.toast.error('El Client ID es obligatorio'); return; }
    if (!this.driveForm.clientSecret.trim() && !this.driveConfig.credencialesGuardadas) {
      this.toast.error('El Client Secret es obligatorio la primera vez'); return;
    }
    if (!this.driveForm.redirectUri.trim()) { this.toast.error('La Redirect URI es obligatoria'); return; }

    this.driveGuardando = true;
    this.http.post<DriveConfigResponse>('/api/backup/drive/config', {
      clientId:     this.driveForm.clientId.trim(),
      clientSecret: this.driveForm.clientSecret.trim() || null,
      redirectUri:  this.driveForm.redirectUri.trim(),
      folderName:   this.driveForm.folderName.trim() || 'SSDC-Backups'
    }).subscribe({
      next: cfg => {
        this.driveConfig       = cfg;
        this.driveGuardando    = false;
        this.driveStep         = 'auth';
        this.driveForm.clientSecret = '';
        this.toast.success('Credenciales guardadas correctamente');
      },
      error: err => {
        this.driveGuardando = false;
        this.toast.error(err?.error?.error ?? 'Error al guardar credenciales');
      }
    });this.cdr.detectChanges();
  }

  abrirAutorizacionGoogle(): void {
    if (!this.driveConfig.authUrl) {
      this.toast.error('No hay URL de autorización. Guarda las credenciales primero.');
      return;
    }
    this.driveAutorizando = true;
    const popup = window.open(
      this.driveConfig.authUrl, 'google-drive-auth',
      'width=520,height=660,left=300,top=80,toolbar=no,menubar=no'
    );
    if (!popup) {
      this.toast.error('El navegador bloqueó el popup. Habilita ventanas emergentes.');
      this.driveAutorizando = false;
      return;
    }
    // Detectar cuando el popup se cierra
    this.authCheckInterval = setInterval(() => {
      if (popup.closed) {
        clearInterval(this.authCheckInterval);
        this.driveAutorizando = false;
        this.cargarDriveConfig();
      }
    }, 1000);
  }

  verificarAutorizacion(): void {
    this.http.get<DriveConfigResponse>('/api/backup/drive/config').subscribe({
      next: cfg => {
        this.driveConfig = cfg;
        this.sincronizarDriveStep(cfg);
        cfg.autorizado
          ? this.toast.success('Google Drive autorizado correctamente')
          : this.toast.error('Aún no se ha completado la autorización');
      }
    });this.cdr.detectChanges();
  }

  revocarAutorizacion(): void {
    if (!confirm('¿Revocar la autorización? Las credenciales se mantendrán.')) return;
    this.http.delete<any>('/api/backup/drive/autorizar').subscribe({
      next: () => {
        this.toast.success('Autorización revocada');
        this.driveStep = 'auth';
        this.driveConfig.autorizado = false;
        this.config.destinoDrive    = false;
      },
      error: () => this.toast.error('Error al revocar')
    });this.cdr.detectChanges();
  }

  eliminarConfigDrive(): void {
    if (!confirm('¿Eliminar TODA la configuración de Google Drive?')) return;
    this.http.delete<any>('/api/backup/drive/config').subscribe({
      next: () => {
        this.toast.success('Configuración eliminada');
        this.driveStep  = 'creds';
        this.driveConfig = {
          id: null, clientIdPreview: null, credencialesGuardadas: false,
          autorizado: false, redirectUri: null, folderName: null, folderId: null,
          expiresAt: null, updatedAt: null, authUrl: null
        };
        this.driveForm       = { clientId: '', clientSecret: '', redirectUri: '', folderName: 'SSDC-Backups' };
        this.config.destinoDrive = false;
      },
      error: () => this.toast.error('Error al eliminar configuración')
    });this.cdr.detectChanges();
  }

  volverAPasoCredenciales(): void { this.driveStep = 'creds'; }

  // ═══════════════════════════════════════════════════════════════
  // EMAIL CHIPS
  // ═══════════════════════════════════════════════════════════════
  syncEmailsFromConfig(): void {
    this.emailChips = this.config.emailDestino?.trim()
      ? this.config.emailDestino.split(/[,;]/).map(e => e.trim()).filter(e => e) : [];
    this.emailInput = ''; this.emailInputError = '';
  }
  syncEmailsToConfig(): void { this.config.emailDestino = this.emailChips.join(', '); }

  agregarEmailChip(): void {
    const e = this.emailInput.trim();
    if (!e) return;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e)) { this.emailInputError = 'Email inválido'; return; }
    if (this.emailChips.includes(e))             { this.emailInputError = 'Ya fue agregado'; return; }
    this.emailChips.push(e);
    this.emailInput = ''; this.emailInputError = '';
    this.syncEmailsToConfig();this.cdr.detectChanges();
  }
  quitarEmailChip(i: number): void { this.emailChips.splice(i, 1); this.syncEmailsToConfig(); }
  onEmailKeydown(e: KeyboardEvent): void {
    if (e.key === 'Enter')     { e.preventDefault(); this.agregarEmailChip(); }
    if (e.key === 'Backspace' && !this.emailInput && this.emailChips.length > 0) {
      this.emailChips.pop(); this.syncEmailsToConfig();
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // RESTAURAR
  // ═══════════════════════════════════════════════════════════════
  onArchivoSeleccionado(e: Event): void {
    this.setFile((e.target as HTMLInputElement).files?.[0] ?? null);
  }
  onDrop(e: DragEvent): void { e.preventDefault(); this.setFile(e.dataTransfer?.files?.[0] ?? null); }
  onDragOver(e: DragEvent): void { e.preventDefault(); this.cdr.detectChanges();}

  private setFile(file: File | null): void {
    this.restaurarFile        = file;
    this.restaurarNombre      = file?.name ?? '';
    this.restaurarEstado      = 'idle';
    this.restaurarMensaje     = '';
    this.restaurarConfirmando = false;
    if (file) this.validarArchivo();this.cdr.detectChanges();
  }

  limpiarArchivo(): void {
    this.restaurarFile        = null;
    this.restaurarNombre      = '';
    this.restaurarEstado      = 'idle';
    this.restaurarMensaje     = '';
    this.restaurarConfirmando = false;
  }

  validarArchivo(): void {
    if (!this.restaurarFile) return;
    this.restaurarEstado = 'validando';
    const form = new FormData();
    form.append('archivo', this.restaurarFile);
    this.http.post<any>('/api/backup/validar', form).subscribe({
      next:  r   => { this.restaurarEstado = r.estado === 'OK' ? 'valido' : 'invalido'; this.restaurarMensaje = r.mensaje; },
      error: err => { this.restaurarEstado = 'invalido'; this.restaurarMensaje = err?.error?.mensaje ?? 'No se pudo validar.'; }
    });this.cdr.detectChanges();
  }

  confirmarRestauracion(): void  { this.restaurarConfirmando = true; }
  cancelarRestauracion(): void   { this.restaurarConfirmando = false; }

  ejecutarRestauracion(): void {
    if (!this.restaurarFile) return;
    this.restaurarEstado      = 'restaurando';
    this.restaurarConfirmando = false;
    const form = new FormData();
    form.append('archivo', this.restaurarFile);
    this.http.post<any>('/api/backup/restaurar', form).subscribe({
      next:  r   => { this.restaurarEstado = 'ok';    this.restaurarMensaje = r.mensaje;                        this.toast.success('BD restaurada correctamente.'); },
      error: err => { this.restaurarEstado = 'error'; this.restaurarMensaje = err?.error?.mensaje ?? 'Error.'; this.toast.error('Error al restaurar.'); }
    });this.cdr.detectChanges();
  }

  // ═══════════════════════════════════════════════════════════════
  // HELPERS UI
  // ═══════════════════════════════════════════════════════════════
  trackByIndex(i: number): number { return i; }

  get horasConfiguradas(): string[] {
    const h = [this.config.horaBackup1];
    if (this.config.numEjecuciones >= 2) h.push(this.config.horaBackup2 || '--:--');
    if (this.config.numEjecuciones >= 3) h.push(this.config.horaBackup3 || '--:--');
    return h.filter(x => x && x !== '--:--')
  }

  get statusLabel(): string {
    if (!this.config.activo) return 'Respaldo automático INACTIVO';
    return `Respaldo automático ACTIVO · ${this.tipoLabel(this.config.tipoBackup)} · ${this.horasConfiguradas.join(', ')}`;this.cdr.detectChanges();
  }

  tipoLabel(t: string): string {
    return ({ FULL: 'Completo', INCREMENTAL: 'Incremental', DIFERENCIAL: 'Diferencial', COMPLETO: 'Completo' })[t?.toUpperCase()] ?? t ?? 'Full';
  }
  tipoExt(h: HistorialBackup): string  { return h.tipoBackupExt ?? h.tipoBackup ?? 'FULL'; }
  tipoCss(t: string): string           { return ({ FULL: 'badge-full', COMPLETO: 'badge-full', INCREMENTAL: 'badge-inc', DIFERENCIAL: 'badge-dif' })[t?.toUpperCase()] ?? 'badge-full'; }
  origenCss(o: string): string         { return o === 'MANUAL' ? 'origen-manual' : 'origen-auto'; }
}
