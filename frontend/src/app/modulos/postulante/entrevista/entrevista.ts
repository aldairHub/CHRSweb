import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';

interface EntrevistaInfo {
  fecha: string;
  hora: string;
  modalidad: 'presencial' | 'virtual';
  enlace?: string;
  lugar?: string;
  evaluadores: string[];
  estado: 'programada' | 'en_curso' | 'completada';
  instrucciones: string[];
}

@Component({
  selector: 'app-entrevista-postulante',
  standalone: true,
  imports: [CommonModule, NavbarComponent, FooterComponent],
  templateUrl: './entrevista.html',
  styleUrls: ['./entrevista.scss']
})
export class EntrevistaPostulanteComponent implements OnInit {

  mostrarModalVideoconf = false;

  postulante = {
    nombre: 'Juan Carlos Pérez Loor',
    proceso: 'Docente No Titular — Área: Tecnologías de la Información'
  };

  entrevista: EntrevistaInfo = {
    fecha: '14 de mayo de 2025',
    hora: '09:00 AM',
    modalidad: 'virtual',
    enlace: 'https://meet.google.com/awx-zsnk-xxa',
    evaluadores: ['Ing. Washington Chiriboga Casanova — Decano FCCDD', 'Ing. Jessica Ponce Ordóñez — Coordinadora de Carrera'],
    estado: 'programada',
    instrucciones: [
      'Conéctese 5 minutos antes de la hora indicada para verificar audio y cámara.',
      'Tenga a la mano todos los documentos de soporte de su hoja de vida.',
      'La clase demostrativa tendrá una duración máxima de 20 minutos.',
      'Prepare el tema indicado por la comisión evaluadora con anticipación.',
      'Asegúrese de estar en un lugar tranquilo con buena conexión a internet.',
      'Vista ropa formal para la sesión de videoconferencia.'
    ]
  };

  get esVirtual(): boolean {
    return this.entrevista.modalidad === 'virtual';
  }

  get colorEstado(): string {
    if (this.entrevista.estado === 'en_curso') return 'en-curso';
    if (this.entrevista.estado === 'completada') return 'completada';
    return 'programada';
  }

  constructor(private router: Router) {}

  ngOnInit(): void {}

  abrirModalVideoconf(): void {
    this.mostrarModalVideoconf = true;
  }

  confirmarUnirse(): void {
    this.mostrarModalVideoconf = false;
    if (this.entrevista.enlace) {
      window.open(this.entrevista.enlace, '_blank');
    }
  }

  volver(): void {
    this.router.navigate(['/postulante']);
  }
}
