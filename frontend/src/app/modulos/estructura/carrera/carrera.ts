import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FooterComponent } from '../../../component/footer.component';
import { NavbarComponent } from '../../../component/navbar';
import { CarreraService } from '../../../services/carrera.service';
import { FacultadService } from '../../../services/facultad.service';
// Asegúrate de importar la interfaz Carrera actualizada
import { Carrera } from '../../../models/carrera.model';

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
export class CarreraComponent implements OnInit {

  carreras: any[] = [];
  facultades: any[] = []; // Guardará objetos { idFacultad: 1, nombreFacultad: '...' }

  search = '';
  filtroFacultad = '';

  modalAbierto = false;
  editando = false;

  // Formulario ajustado para Spring Boot
  form = {
    id: 0,
    idFacultad: null as number | null,
    nombreCarrera: '',
    modalidad: 'presencial',
    estado: true
  };

  constructor(private carreraService: CarreraService,
              private facultadService: FacultadService) {}

  ngOnInit(): void {
    this.cargarCarreras();
    this.cargarFacultades();
  }

  // ===== CARGAR =====
  cargarCarreras() {
    this.carreraService.getAll().subscribe({
      next: data => {
        this.carreras = data;
        // No sobrescribimos 'this.facultades' aquí para no perder los IDs
      },
      error: err => console.error('Error al cargar carreras', err)
    });
  }

  cargarFacultades() {
    this.facultadService.listar().subscribe({
      next: (data: any[]) => {
        // Cargamos la lista completa de objetos
        this.facultades = data;
      },
      error: err => console.error('Error al cargar facultades', err)
    });
  }

  // ===== ESTADÍSTICAS =====
  get carrerasActivas(): number {
    return this.carreras.filter(c => c.estado).length;
  }

  get totalModalidades(): number {
    // Verificamos si existe data antes de mapear
    if(!this.carreras) return 0;
    return new Set(this.carreras.map(c => c.modalidad)).size;
  }

  // ===== FILTRO =====
  carrerasFiltradas(): any[] {
    return this.carreras.filter(c => {
      // Manejo seguro del nombre (puede venir como nombre o nombreCarrera)
      const nombre = c.nombre || c.nombreCarrera || '';
      // Manejo seguro de la facultad (puede ser objeto o string)
      const nombreFacultad = c.facultad?.nombreFacultad || c.facultad || '';

      const matchSearch = nombre.toLowerCase().includes(this.search.toLowerCase());
      const matchFiltro = !this.filtroFacultad || nombreFacultad === this.filtroFacultad;

      return matchSearch && matchFiltro;
    });
  }

  // ===== CREAR =====
  openCreate() {
    this.editando = false;
    this.form = {
      id: 0,
      // Seleccionamos el primer ID disponible o null
      idFacultad: this.facultades.length > 0 ? this.facultades[0].idFacultad : null,
      nombreCarrera: '',
      modalidad: 'presencial',
      estado: true
    };
    this.modalAbierto = true;
  }

  // ===== EDITAR =====
  edit(carrera: any) {
    this.editando = true;

    // Mapeamos los datos recibidos al formato del formulario
    this.form = {
      id: carrera.id || carrera.idCarrera,
      // Intentamos obtener el ID de la facultad si viene anidado
      idFacultad: carrera.facultad?.idFacultad || carrera.idFacultad,
      nombreCarrera: carrera.nombre || carrera.nombreCarrera,
      modalidad: carrera.modalidad,
      estado: carrera.estado
    };
    this.modalAbierto = true;
  }

  // ===== GUARDAR =====
  guardar() {
    console.log("Intentando guardar...", this.form); // 1. Ver qué datos tiene el formulario

    // Validación manual para ver si falta algo
    if (!this.form.nombreCarrera || !this.form.nombreCarrera.trim()) {
      alert("¡Error! El nombre de la carrera está vacío.");
      return;
    }

    if (!this.form.idFacultad) {
      alert("¡Error! No has seleccionado ninguna facultad.");
      return;
    }

    const carreraEnvio = this.form as any;

    if (this.editando) {
      this.carreraService.update(this.form.id, carreraEnvio).subscribe({
        next: () => {
          alert("¡Actualizado con éxito!"); // Feedback visual
          this.cargarCarreras();
          this.closeModal();
        },
        error: err => {
          console.error('Error al actualizar', err);
          alert("Error al actualizar: " + (err.error?.message || err.message));
        }
      });
    } else {
      this.carreraService.create(carreraEnvio).subscribe({
        next: () => {
          alert("¡Creado con éxito!"); // Feedback visual
          this.cargarCarreras();
          this.closeModal();
        },
        error: err => {
          console.error('Error al crear', err);
          alert("Error al crear: Mira la consola (F12) para más detalles");
        }
      });
    }
  }

  // ===== ESTADO =====
  toggleEstado(carrera: any) {
    const id = carrera.id || carrera.idCarrera;
    this.carreraService.toggleEstado(id).subscribe({
      next: () => carrera.estado = !carrera.estado,
      error: err => console.error('Error al cambiar estado', err)
    });
  }

  closeModal() {
    this.modalAbierto = false;
  }
}
