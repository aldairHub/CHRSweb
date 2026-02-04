import { Component, OnInit } from '@angular/core'; // 1. Importamos OnInit
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer.component';
import { MateriaService } from '../../../services/materia.service'; // 2. Asegúrate de que la ruta sea correcta

interface Materia {
  id?: number; // El ID suele ser opcional al crear
  carrera: string;
  carreraId?: number; // Ajustado según tu fragmento de código
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
export class MateriaComponent implements OnInit { // 3. Implementamos OnInit
  search: string = '';
  materias: Materia[] = []; // Empezamos con un array vacío
  modalAbierto = false;
  editando = false;

  form: Materia = {
    id: 0,
    carrera: '',
    nombre: '',
    nivel: 1,
    estado: true
  };

  // 4. Inyectamos el servicio en el constructor
  constructor(private materiaService: MateriaService) {}

  // 5. Al iniciar el componente, traemos los datos del Backend
  ngOnInit(): void {
    this.cargarMaterias();
  }

  cargarMaterias(): void {
    this.materiaService.listar().subscribe({
      next: (data: any) => {
        console.log('Datos recibidos del backend:', data);
        this.materias = data;
      },
      error: (err) => console.error('Error de conexión:', err)
    });
  }
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

  // 6. Lógica de guardado conectada al servicio
  guardar() {
    if (!this.form.nombre.trim()) return;

    if (this.editando) {
      // Aquí podrías llamar a this.materiaService.actualizar(...) si lo tienes
      console.log('Lógica de edición pendiente de conectar al service');
    } else {
      // Usamos el código que proporcionaste para crear
      this.materiaService.crear({
        nombre: this.form.nombre,
        estado: true,
        carreraId: 1 // Ajusta esto según cómo manejes las IDs de carrera
      }).subscribe({
        next: () => {
          this.cargarMaterias(); // Recargamos la lista desde el server
          this.closeModal();
        },
        error: err => console.error('Error al guardar:', err)
      });
    }
  }

  toggleEstado(m: Materia) {
    m.estado = !m.estado;
    // Tip: Aquí también deberías llamar al servicio para que el cambio persista en la DB
  }

  closeModal() {
    this.modalAbierto = false;
  }
}
