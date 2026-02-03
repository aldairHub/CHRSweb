import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FooterComponent } from '../../../component/footer.component';
import { NavbarComponent } from '../../../component/navbar';

interface Carrera {
  id: number;
  facultad: string;
  nombre: string;
  modalidad: 'presencial' | 'virtual' | 'hibrido';
  estado: boolean;
}

@Component({
  selector: 'app-carrera',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './carrera.html',
  styleUrls: ['./carrera.scss']
})
export class CarreraComponent {

  // Facultades simuladas
  facultades: string[] = [
    'Ciencias de la Computación',
    'Ciencias Empresariales',
    'Ingeniería',
    'Ciencias Ambientales'
  ];

  // Datos simulados
  carreras: Carrera[] = [
    { id: 1, facultad: 'Ciencias de la Computación', nombre: 'Ingeniería en Software', modalidad: 'presencial', estado: true },
    { id: 2, facultad: 'Ciencias Empresariales', nombre: 'Marketing Digital', modalidad: 'virtual', estado: true },
    { id: 3, facultad: 'Ingeniería', nombre: 'Ingeniería Civil', modalidad: 'presencial', estado: true },
    { id: 4, facultad: 'Ingeniería', nombre: 'Ingeniería Mecánica', modalidad: 'presencial', estado: false }
  ];

  // Filtros
  search: string = '';
  filtroFacultad: string = '';

  // Modal
  modalAbierto = false;
  editando = false;

  form: Carrera = {
    id: 0,
    facultad: '',
    nombre: '',
    modalidad: 'presencial',
    estado: true
  };

  // ===== Estadísticas =====
  get carrerasActivas(): number {
    return this.carreras.filter(c => c.estado).length;
  }

  get totalModalidades(): number {
    return new Set(this.carreras.map(c => c.modalidad)).size;
  }

  // ===== Filtro =====
  carrerasFiltradas(): Carrera[] {
    return this.carreras.filter(c =>
      c.nombre.toLowerCase().includes(this.search.toLowerCase()) &&
      (!this.filtroFacultad || c.facultad === this.filtroFacultad)
    );
  }

  // ===== Crear =====
  openCreate() {
    this.editando = false;
    this.form = {
      id: 0,
      facultad: this.facultades[0],
      nombre: '',
      modalidad: 'presencial',
      estado: true
    };
    this.modalAbierto = true;
  }

  // ===== Editar =====
  edit(c: Carrera) {
    this.editando = true;
    this.form = { ...c };
    this.modalAbierto = true;
  }

  // ===== Guardar =====
  guardar() {

    if (!this.form.nombre.trim()) return;

    if (this.editando) {
      const i = this.carreras.findIndex(c => c.id === this.form.id);
      this.carreras[i] = { ...this.form };

    } else {
      const nuevoId =
        Math.max(...this.carreras.map(c => c.id), 0) + 1;

      this.carreras.push({
        ...this.form,
        id: nuevoId
      });
    }

    this.closeModal();
  }

  // ===== Toggle =====
  toggleEstado(c: Carrera) {
    c.estado = !c.estado;
  }

  // ===== Cerrar modal =====
  closeModal() {
    this.modalAbierto = false;
  }
}
