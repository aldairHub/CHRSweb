import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer.component';

interface Materia {
  id: number;
  carrera: string;
  nombre: string;
  nivel: number;
  estado: boolean;
}

@Component({
  selector: 'app-materia',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './materia.html',
  styleUrls: ['./materia.scss']
})
export class MateriaComponent {
  search: string = '';


  materias: Materia[] = [
    { id: 1, carrera: 'Ingeniería en Software', nombre: 'Programación OO', nivel: 3, estado: true },
    { id: 2, carrera: 'Ingeniería en Software', nombre: 'Estructuras de Datos', nivel: 4, estado: true },
    { id: 3, carrera: 'Diseño Gráfico', nombre: 'Fundamentos del Diseño', nivel: 1, estado: true },
    { id: 4, carrera: 'Administración', nombre: 'Gestión Estratégica', nivel: 7, estado: false }
  ];

  modalAbierto = false;
  editando = false;

  form: Materia = {
    id: 0,
    carrera: '',
    nombre: '',
    nivel: 1,
    estado: true
  };

  get materiasActivas(): number {
    return this.materias.filter(m => m.estado).length;
  }

  get materiasInactivas(): number {
    return this.materias.filter(m => !m.estado).length;
  }

  openCreate() {
    this.editando = false;
    this.form = { id: 0, carrera: '', nombre: '', nivel: 1, estado: true };
    this.modalAbierto = true;
  }
  materiasFiltradas() {
    return this.materias.filter(m =>
      m.nombre.toLowerCase().includes(this.search.toLowerCase())
    );
  }

  edit(m: Materia) {
    this.editando = true;
    this.form = { ...m };
    this.modalAbierto = true;
  }

  guardar() {

    if (!this.form.nombre.trim() || !this.form.carrera.trim()) return;

    if (this.editando) {
      const i = this.materias.findIndex(m => m.id === this.form.id);
      this.materias[i] = { ...this.form };

    } else {
      const nuevoId = Math.max(...this.materias.map(m => m.id), 0) + 1;
      this.materias.push({ ...this.form, id: nuevoId });
    }

    this.closeModal();
  }

  toggleEstado(m: Materia) {
    m.estado = !m.estado;
  }

  closeModal() {
    this.modalAbierto = false;
  }
}
