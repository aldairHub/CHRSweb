import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-crear-evaluador',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './crear-auto.html',
  styleUrls: ['./crear-auto.scss']
})
export class CrearEvaluadorComponent {

  // Modelo de datos (Coincide con tu tabla autoridad_academica)
  evaluador = {
    nombres: '',
    apellidos: '',
    correo: '',
    fechaNacimiento: '',
    idInstitucion: null,
    // Datos para la tabla usuario
    usuario: '',
    password: ''
  };

  isLoading: boolean = false;

  // Lista simulada de instituciones (Esto vendría de tu BD: public.institucion)
  instituciones = [
    { id: 1, nombre: 'Universidad Técnica Estatal de Quevedo' },
    { id: 2, nombre: 'Universidad de Guayaquil' },
    { id: 3, nombre: 'Pontificia Universidad Católica' }
  ];

  constructor(private router: Router) {}

  generarUsuario() {
    // Pequeña utilidad: genera usuario automático basado en el correo
    if (this.evaluador.correo) {
      this.evaluador.usuario = this.evaluador.correo.split('@')[0];
    }
  }

  guardar() {
    if (!this.validar()) return;

    this.isLoading = true;

    // Simulamos la petición al Backend (INSERT en tablas usuario y autoridad_academica)
    setTimeout(() => {
      this.isLoading = false;
      console.log('Enviando a BD:', this.evaluador);
      alert('Evaluador creado correctamente. Se han enviado las credenciales al correo.');
      this.router.navigate(['/admin']); // Vuelve al dashboard admin
    }, 1000);
  }

  validar(): boolean {
    if (!this.evaluador.nombres || !this.evaluador.apellidos || !this.evaluador.correo ||
      !this.evaluador.fechaNacimiento || !this.evaluador.idInstitucion || !this.evaluador.password) {
      alert('Por favor complete todos los campos obligatorios.');
      return false;
    }
    return true;
  }

  cancelar() {
    this.router.navigate(['/admin']);
  }
}
