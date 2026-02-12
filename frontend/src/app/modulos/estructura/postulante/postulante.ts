import { Component, OnInit } from '@angular/core';
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

  form = {
    nombres: '',
    apellidos: '',
    identificacion: '',
    correo: '',
    estado_revision: 'PENDIENTE',
    observaciones_revision: ''
  };

  constructor() {}

  ngOnInit(): void {}

  guardar() {
    console.log('Postulante registrado:', this.form);
    // luego va this.postulanteService.crear(this.form)
  }
}
