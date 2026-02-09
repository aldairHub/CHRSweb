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

  facultades: Facultad[] = [];
  search: string = '';
  modalAbierto = false;
  editando = false;

  // Formulario actualizado con los nombres correctos
  form: Facultad = {
    idFacultad: 0,
    nombreFacultad: '',
    estado: true
  };

  constructor(private facultadService: FacultadService) {}

  ngOnInit(): void {
    this.cargarFacultades();
  }

  cargarFacultades(): void {
    this.facultadService.listar().subscribe({
      next: (data: any[]) => {
        this.facultades = data;
        console.log("Facultades cargadas:", data); // Para depurar
      },
      error: (err) => console.error('Error al cargar:', err)
    });
  }

  // ===== ESTADÍSTICAS =====
  get facultadesActivas(): number {
    return this.facultades.filter(f => f.estado).length;
  }

  get facultadesInactivas(): number {
    return this.facultades.filter(f => !f.estado).length;
  }

  // ===== FILTROS (Aquí estaba el error) =====
  facultadesFiltradas(): Facultad[] {
    return this.facultades.filter(f =>
      // Usamos 'nombreFacultad' y validamos que exista
      (f.nombreFacultad || '').toLowerCase().includes(this.search.toLowerCase())
    );
  }

  // ===== MODAL =====
  openCreate(): void {
    this.editando = false;
    this.form = { idFacultad: 0, nombreFacultad: '', estado: true };
    this.modalAbierto = true;
  }

  edit(f: Facultad): void {
    this.editando = true;
    this.form = { ...f }; // Copia los datos al formulario
    this.modalAbierto = true;
  }

  closeModal(): void {
    this.modalAbierto = false;
  }

  // ===== ACCIONES =====
  guardar(): void {
    if (!this.form.nombreFacultad || !this.form.nombreFacultad.trim()) {
      alert("El nombre de la facultad es obligatorio");
      return;
    }

    const payload = {
      nombre_facultad: this.form.nombreFacultad,
      estado: this.form.estado
    };

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
        alert("Error: " + (err.error?.message || "Revisa la consola"));
      }
    });
  }


  toggleEstado(f: Facultad): void {
    f.estado = !f.estado; // Cambio visual inmediato
    this.facultadService.actualizar(f.idFacultad, f as any).subscribe({
      error: (err) => {
        f.estado = !f.estado; // Revertir si falla
        console.error('Error al cambiar estado', err);
      }
    });
  }
}
