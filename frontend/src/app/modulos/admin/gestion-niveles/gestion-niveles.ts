import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer.component';
import { ToastComponent } from '../../../component/toast.component';
import { ToastService } from '../../../services/toast.service';
import { NivelAcademicoService, NivelAcademico } from '../../../services/nivel-academico.service';

@Component({
  selector: 'app-gestion-niveles',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, FooterComponent, ToastComponent],
  templateUrl: './gestion-niveles.html',
  styleUrls: ['./gestion-niveles.scss']
})
export class GestionNivelesComponent implements OnInit {
  cargando = false;

  niveles: NivelAcademico[] = [];
  search = '';

  modalAbierto = false;
  editando     = false;
  confirmando  = false;
  nivelAEliminar: NivelAcademico | null = null;

  form: Partial<NivelAcademico> = { nombre: '', orden: 0, estado: true };

  constructor(
    private svc: NivelAcademicoService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void { this.cargar(); }

  // ── Carga ─────────────────────────────────────────────────
  cargar(): void {
    this.cargando = true;
    this.svc.listar().subscribe({
      next: data => { this.niveles = data; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.cargando = false; this.toast.error('Error', 'No se pudieron cargar los niveles académicos.'); }
    });
  }

  // ── Stats ─────────────────────────────────────────────────
  get activos():   number { return this.niveles.filter(n => n.estado).length; }
  get inactivos(): number { return this.niveles.filter(n => !n.estado).length; }

  // ── Filtro ────────────────────────────────────────────────
  filtrados(): NivelAcademico[] {
    const q = this.search.toLowerCase();
    return this.niveles.filter(n => n.nombre.toLowerCase().includes(q));
  }

  // ── Modal crear ───────────────────────────────────────────
  openCreate(): void {
    this.editando = false;
    this.form = { nombre: '', orden: this.niveles.length + 1, estado: true };
    this.modalAbierto = true;
  }

  // ── Modal editar ──────────────────────────────────────────
  edit(n: NivelAcademico): void {
    this.editando = true;
    this.form = { ...n };
    this.modalAbierto = true;
  }

  closeModal(): void { this.modalAbierto = false; }

  // ── Guardar ───────────────────────────────────────────────
  guardar(): void {
    const nombre = (this.form.nombre ?? '').trim();
    if (!nombre) {
      this.toast.warning('Campo requerido', 'El nombre del nivel es obligatorio.');
      return;
    }
    this.form.nombre = nombre;

    const request = this.editando
      ? this.svc.actualizar(this.form['idNivel'] as number, this.form)
      : this.svc.crear(this.form);

    const loadId = this.toast.loading(this.editando ? 'Actualizando...' : 'Creando...');
    request.subscribe({
      next: () => {
        this.toast.remove(loadId);
        this.toast.success(
          this.editando ? 'Nivel actualizado' : 'Nivel creado',
          `"${nombre}" fue ${this.editando ? 'actualizado' : 'creado'} correctamente.`
        );
        this.cargar();
        this.closeModal();
      },
      error: err => {
        this.cargando = false;
        this.toast.remove(loadId);
        this.toast.error('Error', err?.error?.message || 'Intenta de nuevo.');
      }
    });
  }

  // ── Toggle estado ─────────────────────────────────────────
  toggleEstado(n: NivelAcademico): void {
    const anterior = n.estado;
    n.estado = !n.estado;
    this.svc.actualizar(n.idNivel, { nombre: n.nombre, orden: n.orden, estado: n.estado }).subscribe({
      next: () => this.toast.info(
        n.estado ? 'Nivel activado' : 'Nivel desactivado',
        `"${n.nombre}" fue ${n.estado ? 'activado' : 'desactivado'}.`
      ),
      error: err => {
        this.cargando = false;
        n.estado = anterior;
        this.toast.error('Error', err?.error?.message || 'No se pudo cambiar el estado.');
      }
    });
  }

  // ── Eliminar con confirmación ─────────────────────────────
  confirmarEliminar(n: NivelAcademico): void {
    this.nivelAEliminar = n;
    this.confirmando = true;
  }

  cancelarEliminar(): void {
    this.confirmando = false;
    this.nivelAEliminar = null;
  }

  eliminar(): void {
    if (!this.nivelAEliminar) return;
    const nombre = this.nivelAEliminar.nombre;
    const loadId = this.toast.loading('Eliminando...');
    this.svc.eliminar(this.nivelAEliminar.idNivel).subscribe({
      next: () => {
        this.toast.remove(loadId);
        this.toast.success('Eliminado', `"${nombre}" fue eliminado.`);
        this.cancelarEliminar();
        this.cargar();
      },
      error: err => {
        this.cargando = false;
        this.toast.remove(loadId);
        this.toast.error('Error', err?.error?.message || 'No se pudo eliminar.');
        this.cancelarEliminar();
      }
    });
  }
}
