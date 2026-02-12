import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer.component';
@Component({
  selector: 'app-terna',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './terna.html',
  styleUrls: ['./terna.scss']
})
export class TernaComponent implements OnInit {

  // === MODELO ===
  form = {
    idArea: null,
    presidente: null,
    miembro1: null,
    miembro2: null
  };

  // === LISTAS ===
  areas: any[] = [];
  autoridades: any[] = [];

  constructor() {}

  ngOnInit(): void {
    this.cargarAreas();
    this.cargarAutoridades();
  }

  cargarAreas() {
    // 🔧 luego conectas al backend
    this.areas = [
      { id_area: 1, nombre_area: 'Ingeniería de Software' },
      { id_area: 2, nombre_area: 'Ciencias Básicas' }
    ];
  }

  cargarAutoridades() {
    // 🔧 luego conectas al backend
    this.autoridades = [
      { id_autoridad: 1, nombres: 'Juan', apellidos: 'Pérez' },
      { id_autoridad: 2, nombres: 'María', apellidos: 'Gómez' },
      { id_autoridad: 3, nombres: 'Carlos', apellidos: 'Mendoza' }
    ];
  }

  guardar() {
    console.log('Terna registrada:', this.form);
    // aquí luego va el service.post(...)
  }
}
