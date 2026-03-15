import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioService } from '../../services/usuario.service';
import { ToastService } from '../../services/toast.service';
import { ToastComponent } from '../../component/toast.component';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastComponent],
  templateUrl: './perfil.html',
  styleUrls: ['./perfil.scss']
})
export class PerfilComponent implements OnInit {

  // ─── Datos usuario ─────────────────────────────────────────
  nombreUsuario = localStorage.getItem('usuario') || '';
  rolUsuario    = localStorage.getItem('rol') || '';

  // ─── Formulario cambio de clave ────────────────────────────
  claveActual            = '';
  claveNueva             = '';
  claveNuevaConfirmacion = '';

  showClaveActual   = false;
  showClaveNueva    = false;
  showClaveConfirm  = false;

  isLoading = false;
  error     = '';
  exito     = '';

  // ─── Foto de perfil ────────────────────────────────────────
  fotoPerfil:      string | null = null;   // URL cargada desde el backend
  fotoPreview:     string | null = null;   // preview local (FileReader)
  archivoFoto:     File   | null = null;   // archivo seleccionado
  subiendoFoto     = false;
  errorFoto        = '';

  private readonly MAX_SIZE = 2 * 1024 * 1024; // 2 MB

  @ViewChild('inputFoto') inputFoto!: ElementRef<HTMLInputElement>;

  constructor(
    private usuarioSvc: UsuarioService,
    private toast:      ToastService,
    private cdr:        ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Cargar perfil desde el backend para obtener la foto actual
    this.usuarioSvc.obtenerMiPerfil().subscribe({
      next: perfil => {
        this.fotoPerfil = perfil.fotoPerfil;
        this.cdr.detectChanges();
      },
      error: () => {
        // Si falla, intentar desde localStorage (guardado por el servicio)
        this.fotoPerfil = localStorage.getItem('foto_perfil') || null;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Clave ────────────────────────────────────────────────────
  toggleClaveActual():  void { this.showClaveActual  = !this.showClaveActual; }
  toggleClaveNueva():   void { this.showClaveNueva   = !this.showClaveNueva; }
  toggleClaveConfirm(): void { this.showClaveConfirm = !this.showClaveConfirm; }

  onSubmit(): void {
    this.error = '';
    this.exito = '';

    if (!this.claveActual || !this.claveNueva || !this.claveNuevaConfirmacion) {
      this.error = 'Completa todos los campos.'; return;
    }
    if (this.claveNueva.length < 8) {
      this.error = 'La nueva contraseña debe tener al menos 8 caracteres.'; return;
    }
    if (this.claveNueva !== this.claveNuevaConfirmacion) {
      this.error = 'Las contraseñas nuevas no coinciden.'; return;
    }
    if (this.claveActual === this.claveNueva) {
      this.error = 'La nueva contraseña debe ser diferente a la actual.'; return;
    }

    this.isLoading = true;
    this.cdr.detectChanges();

    this.usuarioSvc.cambiarClave(this.claveActual, this.claveNueva, this.claveNuevaConfirmacion)
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.exito = 'Contraseña actualizada correctamente.';
          this.limpiarFormulario();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.isLoading = false;
          this.error = (typeof err?.error === 'string' && err.error.length < 200)
            ? err.error
            : 'No se pudo actualizar la contraseña. Intenta de nuevo.';
          this.cdr.detectChanges();
        }
      });
  }

  private limpiarFormulario(): void {
    this.claveActual = this.claveNueva = this.claveNuevaConfirmacion = '';
  }

  // ── Foto de perfil ────────────────────────────────────────────

  /** Abre el input file oculto */
  abrirSelectorFoto(): void {
    this.inputFoto?.nativeElement.click();
  }

  /** Al seleccionar archivo: validar y mostrar preview local */
  onFotoSeleccionada(event: Event): void {
    this.errorFoto = '';
    const input = event.target as HTMLInputElement;
    const file  = input.files?.[0];
    if (!file) return;

    // Validación frontend
    if (!file.type.startsWith('image/')) {
      this.errorFoto = 'Solo se permiten archivos de imagen (JPG, PNG, WebP…)';
      input.value   = '';
      this.cdr.detectChanges();
      return;
    }
    if (file.size > this.MAX_SIZE) {
      this.errorFoto = 'El archivo supera el límite de 2 MB.';
      input.value   = '';
      this.cdr.detectChanges();
      return;
    }

    this.archivoFoto = file;

    // Preview inmediato con FileReader
    const reader = new FileReader();
    reader.onload = (e) => {
      this.fotoPreview = e.target?.result as string;
      this.cdr.detectChanges();
    };
    reader.readAsDataURL(file);
  }

  /** Sube la foto al backend */
  guardarFoto(): void {
    if (!this.archivoFoto) return;
    this.subiendoFoto = true;
    this.errorFoto    = '';
    this.cdr.detectChanges();

    this.usuarioSvc.subirFotoPerfil(this.archivoFoto).subscribe({
      next: res => {
        this.fotoPerfil   = res.fotoPerfil;
        this.fotoPreview  = null;    // descartamos el preview local
        this.archivoFoto  = null;
        this.subiendoFoto = false;
        if (this.inputFoto) this.inputFoto.nativeElement.value = '';
        this.toast.success('Foto actualizada', 'Tu foto de perfil se guardó correctamente.');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.subiendoFoto = false;
        this.errorFoto = err.error?.message || err.message || 'Error al subir la foto.';
        this.toast.error('Error', this.errorFoto);
        this.cdr.detectChanges();
      }
    });
  }

  /** Cancela la selección sin subir */
  cancelarFoto(): void {
    this.fotoPreview = null;
    this.archivoFoto = null;
    this.errorFoto   = '';
    if (this.inputFoto) this.inputFoto.nativeElement.value = '';
    this.cdr.detectChanges();
  }

  /** URL a mostrar: preview local > foto del backend > null (iniciales) */
  get fotoActual(): string | null {
    return this.fotoPreview || this.fotoPerfil || null;
  }
}
