import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar';
import { TipoDocumentoService, TipoDocumento } from '../../../services/tipo-documento.service';

@Component({
  selector: 'app-gestion-documentos',
  standalone: true,
  templateUrl: './gestion-documentos.html',
  styleUrls: ['./gestion-documentos.scss'],
  imports: [CommonModule, FormsModule, NavbarComponent]
})
export class GestionDocumentosComponent implements OnInit {

  tiposDocumento: TipoDocumento[] = [];
  showTipoModal = false;
  tipoEditando: TipoDocumento | null = null;
  tipoForm = { nombre: '', descripcion: '', obligatorio: false };

  // ── Toast ──────────────────────────────────────────────
  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  constructor(
    private tipoDocumentoService: TipoDocumentoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarTipos();
  }

  cargarTipos(): void {
    this.tipoDocumentoService.listar().subscribe({
      next: data => {
        this.tiposDocumento = data;
        this.cdr.detectChanges();
      },
      error: () => this.mostrarToast('error', 'Error al cargar tipos de documento')
    });
  }

  abrirModalCrear(): void {
    this.tipoEditando = null;
    this.tipoForm = { nombre: '', descripcion: '', obligatorio: false };
    this.showTipoModal = true;
  }

  abrirModalEditar(tipo: TipoDocumento): void {
    this.tipoEditando = tipo;
    this.tipoForm = {
      nombre: tipo.nombre,
      descripcion: tipo.descripcion || '',
      obligatorio: tipo.obligatorio
    };
    this.showTipoModal = true;
  }

  closeTipoModal(): void {
    this.showTipoModal = false;
    this.tipoEditando = null;
  }

  guardarTipo(): void {
    if (!this.tipoForm.nombre.trim()) return;

    if (this.tipoEditando) {
      this.tipoDocumentoService.editar(this.tipoEditando.idTipoDocumento, this.tipoForm).subscribe({
        next: res => {
          if (res.exitoso) {
            this.mostrarToast('success', 'Tipo actualizado correctamente');
            this.closeTipoModal();
            this.cargarTipos();
          } else {
            this.mostrarToast('error', res.mensaje || 'Error al actualizar');
          }
        },
        error: () => this.mostrarToast('error', 'Error al actualizar el tipo')
      });
    } else {
      this.tipoDocumentoService.crear(this.tipoForm).subscribe({
        next: res => {
          const msg = res.mensaje || '';
          if (msg.startsWith('ERROR')) {
            this.mostrarToast('error', msg.replace('ERROR: ', ''));
          } else {
            this.mostrarToast('success', 'Tipo creado correctamente');
            this.closeTipoModal();
            this.cargarTipos();
          }
        },
        error: () => this.mostrarToast('error', 'Error al crear el tipo')
      });
    }
  }

  toggleTipo(tipo: TipoDocumento): void {
    this.tipoDocumentoService.toggle(tipo.idTipoDocumento).subscribe({
      next: res => {
        if (res.exitoso) {
          tipo.activo = res.activo;
          this.mostrarToast('success', `Tipo ${res.activo ? 'activado' : 'desactivado'}`);
          this.cdr.detectChanges();
        }
      },
      error: () => this.mostrarToast('error', 'Error al cambiar estado')
    });
  }

  mostrarToast(tipo: 'success' | 'error', mensaje: string): void {
    this.toastType = tipo;
    this.toastMessage = mensaje;
    this.showToast = true;
    clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => {
      this.showToast = false;
      this.cdr.detectChanges();
    }, 2500);
  }
}
