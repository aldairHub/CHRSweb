import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FooterComponent } from '../../../component/footer.component';
import { NavbarComponent } from '../../../component/navbar';


interface Facultad {
  id: number;
  nombre: string;
  estado: boolean;
}

@Component({
  selector: 'app-facultad',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './facultad.html',
  styleUrls: ['./facultad.scss']
})

export class FacultadComponent {

  // ================================
  // Datos simulados (luego backend)
  // ================================
  facultades: Facultad[] = [
    { id: 1, nombre: 'Ciencias de la Computación', estado: true },
    { id: 2, nombre: 'Ciencias Empresariales', estado: true },
    { id: 3, nombre: 'Ingeniería', estado: true },
    { id: 4, nombre: 'Ciencias Ambientales', estado: false }
  ];

  // ================================
  // Búsqueda
  // ================================
  search: string = '';

  // ================================
  // Modal
  // ================================
  modalAbierto = false;
  editando = false;

  form: Facultad = {
    id: 0,
    nombre: '',
    estado: true
  };

  // ================================
  // Estadísticas
  // ================================
  get facultadesActivas(): number {
    return this.facultades.filter(f => f.estado).length;
  }

  get facultadesInactivas(): number {
    return this.facultades.filter(f => !f.estado).length;
  }

  // ================================
  // Filtro
  // ================================
  facultadesFiltradas(): Facultad[] {
    return this.facultades.filter(f =>
      f.nombre.toLowerCase().includes(this.search.toLowerCase())
    );
  }

  // ================================
  // Crear nueva facultad
  // ================================
  openCreate(): void {
    this.editando = false;
    this.form = { id: 0, nombre: '', estado: true };
    this.modalAbierto = true;
  }

  // ================================
  // Editar facultad
  // ================================
  edit(f: Facultad): void {
    this.editando = true;
    this.form = { ...f };
    this.modalAbierto = true;
  }

  // ================================
  // Guardar (crear o editar)
  // ================================
  guardar(): void {

    if (!this.form.nombre.trim()) return;

    if (this.editando) {
      const i = this.facultades.findIndex(f => f.id === this.form.id);
      if (i !== -1) {
        this.facultades[i] = { ...this.form };
      }

    } else {
      const nuevoId =
        Math.max(0, ...this.facultades.map(f => f.id)) + 1;

      this.facultades.push({
        ...this.form,
        id: nuevoId
      });
    }

    this.closeModal();
  }

  // ================================
  // Activar / desactivar
  // ================================
  toggleEstado(f: Facultad): void {
    f.estado = !f.estado;
  }

  // ================================
  // Cerrar modal
  // ================================
  closeModal(): void {
    this.modalAbierto = false;
  }

}
