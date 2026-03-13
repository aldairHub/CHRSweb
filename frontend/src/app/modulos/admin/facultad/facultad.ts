import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ToastComponent } from '../../../component/toast.component';
import { ToastService } from '../../../services/toast.service';
import { FacultadService } from '../../../services/facultad.service';
import { Facultad } from '../../../models/facultad.model';
@Component({
  selector: 'app-facultad',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastComponent],
  templateUrl: './facultad.html',
  styleUrls: ['./facultad.scss']
})
export class FacultadComponent implements OnInit {

  // ===== Tabla =====
  cargando = false;
  facultades: Facultad[] = [];

  // ===== Filtros =====
  search: string = '';

  // ===== Modales =====
  modalAbierto = false;
  editando = false;

  // ===== Formulario =====
  form: Facultad = {
    idFacultad: 0,
    nombreFacultad: '',
    estado: true
  };

  constructor(
    private facultadService: FacultadService,
    private cdr: ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.cargarFacultades();
  }

  // =========================
  // LOADERS BACKEND
  // =========================
  cargarFacultades(): void {
    this.cargando = true;
    this.facultadService.listar().subscribe({
      next: (data: any[]) => {
        this.cargando = false;
        this.cdr.detectChanges();
        this.facultades = data.map(x => ({
          idFacultad: x.idFacultad ?? x.id_facultad,
          nombreFacultad: x.nombreFacultad ?? x.nombre_facultad,
          estado: x.estado
        }));
        this.cdr.detectChanges();
      },
      error: () => this.toast.error('Error', 'No se pudieron cargar las facultades.')
    });
  }

  // =========================
  // ESTADÍSTICAS
  // =========================
  get facultadesActivas(): number {
    return this.facultades.filter(f => !!f.estado).length;
  }

  get facultadesInactivas(): number {
    return this.facultades.filter(f => !f.estado).length;
  }

  // =========================
  // FILTROS
  // =========================
  facultadesFiltradas(): Facultad[] {
    const q = (this.search ?? '').toLowerCase();
    return this.facultades.filter(f =>
      ((f.nombreFacultad ?? '')).toLowerCase().includes(q)
    );
  }

  // =========================
  // MODAL CREAR
  // =========================
  openCreate(): void {
    this.editando = false;
    this.form = { idFacultad: 0, nombreFacultad: '', estado: true };
    this.modalAbierto = true;
  }

  // =========================
  // MODAL EDITAR
  // =========================
  edit(f: Facultad): void {
    this.editando = true;
    this.form = { ...f };
    this.modalAbierto = true;
  }

  closeModal(): void {
    this.modalAbierto = false;
  }

  // =========================
  // MAPEADOR
  // =========================
  private toPayload(f: Facultad) {
    return {
      nombre_facultad: (f.nombreFacultad ?? '').trim(),
      estado: !!f.estado
    };
  }

  // =========================
  // GUARDAR
  // =========================
  guardar(): void {
    const nombre = (this.form.nombreFacultad ?? '').trim();
    if (!nombre) {
      this.toast.warning('Campo requerido', 'El nombre de la facultad es obligatorio.');
      return;
    }

    // Aseguramos el trim en el form también
    this.form.nombreFacultad = nombre;
    const payload = { nombreFacultad: this.form.nombreFacultad, estado: this.form.estado };
    const request = this.editando
      ? this.facultadService.actualizar(this.form.idFacultad, payload)
      : this.facultadService.crear(payload);

    const loadId = this.toast.loading(this.editando ? 'Actualizando...' : 'Creando...');
    request.subscribe({
      next: () => {
        this.toast.remove(loadId);
        this.toast.success(
          this.editando ? 'Facultad actualizada' : 'Facultad creada',
          `"${nombre}" fue ${this.editando ? 'actualizada' : 'creada'} correctamente.`
        );
        this.cargarFacultades();
        this.closeModal();
      },
      error: (err) => {
        this.cargando = false;
        this.cdr.detectChanges();
        this.toast.remove(loadId);
        this.toast.error('No se pudo guardar', err?.error?.message || 'Intenta de nuevo.');
      }
    });
  }

  // =========================
  // TOGGLE STATUS
  // =========================
  toggleEstado(f: Facultad): void {
    const anterior = f.estado;
    f.estado = !f.estado;
    this.facultadService.actualizar(f.idFacultad, this.toPayload(f)).subscribe({
      next: () => this.toast.info(
        f.estado ? 'Facultad activada' : 'Facultad desactivada',
        `"${f.nombreFacultad}" fue ${f.estado ? 'activada' : 'desactivada'}.`
      ),
      error: (err) => {
        this.cargando = false;
        this.cdr.detectChanges();
        f.estado = anterior;
        this.toast.error('Error', err?.error?.message || 'No se pudo cambiar el estado.');
      }
    });
  }
}
