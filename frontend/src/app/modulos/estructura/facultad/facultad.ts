import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FooterComponent } from '../../../component/footer.component';
import { NavbarComponent } from '../../../component/navbar';
import { FacultadService } from '../../../services/facultad.service';
import { Facultad } from '../../../models/facultad.model';

@Component({
  selector: 'app-facultad',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, FooterComponent],
  templateUrl: './facultad.html',
  styleUrls: ['./facultad.scss']
})
export class FacultadComponent implements OnInit {

  // ===== Tabla =====
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

  constructor(private facultadService: FacultadService) {}

  ngOnInit(): void {
    this.cargarFacultades();
  }

  // =========================
  // LOADERS BACKEND
  // =========================
  cargarFacultades(): void {
    this.facultadService.listar().subscribe({
      next: (data: any[]) => {
        this.facultades = data.map(x => ({
          idFacultad: x.idFacultad ?? x.id_facultad,
          nombreFacultad: x.nombreFacultad ?? x.nombre_facultad,
          estado: x.estado
        }));
        console.log('Facultades cargadas (mapeadas):', this.facultades);
      },
      error: (err) => console.error('Error al cargar:', err)
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
  // MAPEADOR (CLAVE PARA QUE NO MANDE NULL)
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
      alert('El nombre de la facultad es obligatorio');
      return;
    }

    // Aseguramos el trim en el form también
    this.form.nombreFacultad = nombre;

    const payload = {
      nombreFacultad: this.form.nombreFacultad,
      estado: this.form.estado
    };

    console.log('Payload enviado (guardar):', payload);

    const request = this.editando
      ? this.facultadService.actualizar(this.form.idFacultad, payload)
      : this.facultadService.crear(payload);

    request.subscribe({
      next: () => {
        this.cargarFacultades();
        this.closeModal();
      },
      error: (err) => {
        console.error('Error al guardar:', err);
        alert('Error: ' + (err.error?.message || 'Revisa la consola'));
      }
    });
  }

  // =========================
  // TOGGLE STATUS
  // =========================
  toggleEstado(f: Facultad): void {
    const anterior = f.estado;
    f.estado = !f.estado; // cambio visual inmediato

    // IMPORTANTÍSIMO: no mandes "f" (camelCase) al backend
    const payload = this.toPayload(f);
    console.log('Payload enviado (toggleEstado):', payload);

    this.facultadService.actualizar(f.idFacultad, payload).subscribe({
      error: (err) => {
        f.estado = anterior; // revertir si falla
        console.error('Error al cambiar estado', err);
      }
    });
  }
}
