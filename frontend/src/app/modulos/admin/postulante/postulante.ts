import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';

@Component({
  selector: 'app-postulante', standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './postulante.html',
  styleUrls: ['./postulante.scss']
})
export class PostulanteComponent implements OnInit {
  cargando = false;

  form = {
    nombres: '',
    apellidos: '',
    identificacion: '',
    correo: '',
    estado_revision: 'PENDIENTE',
    observaciones_revision: ''
  };

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {}

  guardar() {
    console.log('postulante registrado:', this.form);
    // luego va this.postulanteService.crear(this.form)
  }
}
