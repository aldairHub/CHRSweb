// config-institucion.ts
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastComponent } from '../../../component/toast.component';
import { ToastService } from '../../../services/toast.service';
import { InstitucionAdminService, InstitucionConfig } from '../../../services/institucion-admin.service';
import { LogoService } from '../../../services/logo.service';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-config-institucion',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastComponent],
  templateUrl: './config-institucion.html',
  styleUrls: ['./config-institucion.scss']
})
export class ConfigInstitucionComponent implements OnInit {

  config: InstitucionConfig | null = null;
  isLoading = false;
  isSaving = false;

  form = {
    nombreInstitucion: '',
    direccion: '',
    correo: '',
    telefono: '',
    appName: '',
    emailSmtp: '',
    gmailPassword: '',
    emailHost: 'smtp.gmail.com',
    emailPort: 587,
    emailSsl: false,
    imagenFondoUrl: '',
    nombreCorto: '',

  };
  escudoFile: File | null = null;
  escudoPreview: string | null = null;
  descargandoPreview = false;

  logoFile: File | null = null;
  logoPreview: string | null = null;

  constructor(
    private svc:        InstitucionAdminService,
    private cdr:        ChangeDetectorRef,
    private toast:      ToastService,
    private logoService: LogoService
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.isLoading = true;
    this.svc.obtenerActiva().subscribe({
      next: (data) => {
        this.config = data;
        this.prellenarForm(data);
        this.isLoading = false;
        this.cdr.detectChanges(); // forzar actualización de la vista
      },
      error: () => {
        this.mostrarMensaje('danger', 'No se pudo cargar la configuración institucional.');
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private prellenarForm(data: InstitucionConfig): void {
    this.form.nombreInstitucion = data.nombreInstitucion ?? '';
    this.form.direccion         = data.direccion         ?? '';
    this.form.correo            = data.correo            ?? '';
    this.form.telefono          = data.telefono          ?? '';
    this.form.appName           = data.appName           ?? '';
    this.form.emailSmtp         = data.emailSmtp         ?? '';
    this.form.emailHost         = data.emailHost         ?? 'smtp.gmail.com';
    this.form.emailPort         = data.emailPort         ?? 587;
    this.form.emailSsl          = data.emailSsl          ?? false;
    this.form.gmailPassword     = '';
    this.form.imagenFondoUrl = data.imagenFondoUrl ?? '';
    this.form.nombreCorto    = (data as any).nombreCorto  ?? '';
  }

  onLogoSeleccionado(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    if (file.size > 2 * 1024 * 1024) {
      this.mostrarMensaje('danger', 'El logo no puede superar 2 MB.');
      return;
    }

    this.logoFile = file;
    const reader = new FileReader();
    reader.onload = (e) => {
      this.logoPreview = e.target?.result as string;
      this.cdr.detectChanges(); // FileReader corre fuera de la zona de Angular
    };
    reader.readAsDataURL(file);
  }
  onEscudoSeleccionado(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    if (file.size > 2 * 1024 * 1024) {
      this.mostrarMensaje('danger', 'El escudo no puede superar 2 MB.');
      return;
    }

    this.escudoFile = file;
    const reader = new FileReader();
    reader.onload = (e) => {
      this.escudoPreview = e.target?.result as string;
      this.cdr.detectChanges();
    };
    reader.readAsDataURL(file);
  }
  guardar(): void {
    if (!this.config) return;

    if (!this.form.nombreInstitucion?.trim()) {
      this.mostrarMensaje('danger', 'El nombre de la institución es obligatorio.');
      return;
    }

    this.isSaving = true;

    const payload: any = { ...this.form };
    if (!payload.gmailPassword?.trim()) {
      delete payload.gmailPassword;
    }

    this.svc.actualizar(this.config.idInstitucion, payload)
      .pipe(finalize(() => {
        this.isSaving = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (updated) => {
          this.config = updated;
          const idLogo = updated?.idInstitucion ?? this.config?.idInstitucion;

          if (this.logoFile && idLogo) {
            this.isSaving = true;
            this.svc.uploadLogo(idLogo, this.logoFile)
              .pipe(finalize(() => {
                this.isSaving = false;
                this.cdr.detectChanges();
              }))
              .subscribe({
                next: (res) => {
                  this.config!.logoUrl = res.logoUrl;
                  this.logoFile = null;
                  this.logoService.cargar();
                  this.mostrarMensaje('success', 'Configuración y logo guardados correctamente.');
                },
                error: () => {
                  this.mostrarMensaje('warning', 'Datos guardados, pero hubo un error al subir el logo.');
                }
              });
          }
          if (this.escudoFile && idLogo) {
            this.svc.uploadEscudo(idLogo, this.escudoFile).subscribe({
              next: (res) => {
                this.config!.escudoUrl = res.escudoUrl;
                this.escudoFile = null;
                this.mostrarMensaje('success', 'Escudo guardado correctamente.');
              },
              error: () => {
                this.mostrarMensaje('warning', 'Datos guardados, pero hubo un error al subir el escudo.');
              }
            });
          }
          else {
            this.logoService.cargar();
            this.mostrarMensaje('success', 'Configuración guardada correctamente.');
          }
        },
        error: (err) => {
          const msg = err?.error?.message ?? err?.message ?? 'Error desconocido';
          this.mostrarMensaje('danger', 'Error al guardar: ' + msg);
        }
      });
  }

  resetForm(): void {
    if (!this.config) return;
    this.prellenarForm(this.config);
    this.logoFile = null;
    this.logoPreview = null;
    this.escudoFile = null;
    this.escudoPreview = null;
  }
  previewActa(): void {
    window.open('http://localhost:8080/api/instituciones/preview/acta-pdf', '_blank');
  }

  previewInforme(): void {
    window.open('http://localhost:8080/api/instituciones/preview/informe-pdf', '_blank');
  }
  // ── Mensaje ────────────────────────────────────────────────
  // Delegamos al ToastService centralizado. Mantenemos la firma para no tocar ninguna de las llamadas existentes en este componente.
  private mostrarMensaje(tipo: string, texto: string): void {
    if (tipo === 'success') this.toast.success('Éxito', texto);
    else if (tipo === 'warning') this.toast.warning('Atención', texto);
    else this.toast.error('Error', texto);
  }
}
