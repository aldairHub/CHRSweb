import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';

// =====================================================
// INTERFACES
// =====================================================
export interface CriterioEntrevista {
  id: string;
  label: string;
  max: number;
  grupo: 'personalidad' | 'clase';
}

export interface EntrevistaCandidato {
  id: number;
  nombre: string;
  apellido: string;
  hora: string;
  modalidad: 'presencial' | 'virtual';
  enlace?: string;
  estado: 'pendiente' | 'en_curso' | 'completado';
  foto: string;
  observaciones: string;
  puntajes: { [key: string]: number };
  totalPersonalidad: number;
  totalClase: number;
  totalEntrevista: number;
}

@Component({
  selector: 'app-entrevistas',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, FooterComponent],
  templateUrl: './entrevistas.html',
  styleUrls: ['./entrevistas.scss']
})
export class EntrevistasComponent implements OnInit {

  // =====================================================
  // CANDIDATOS DE LA TERNA
  // =====================================================
  candidatos: EntrevistaCandidato[] = [
    {
      id: 1,
      nombre: 'VALERIA DAYANNA',
      apellido: 'TORRES LINDAO',
      hora: '09:00 AM',
      modalidad: 'virtual',
      enlace: 'https://meet.google.com/awx-zsnk-xxa',
      estado: 'pendiente',
      foto: 'https://ui-avatars.com/api/?name=Valeria+Torres&background=16a34a&color=fff&bold=true',
      observaciones: '',
      puntajes: {},
      totalPersonalidad: 0,
      totalClase: 0,
      totalEntrevista: 0
    },
    {
      id: 2,
      nombre: 'JEAN CARLOS',
      apellido: 'ALMEIDA MURILLO',
      hora: '10:30 AM',
      modalidad: 'virtual',
      enlace: 'https://meet.google.com/xyz-abcd-efg',
      estado: 'completado',
      foto: 'https://ui-avatars.com/api/?name=Jean+Almeida&background=0d5e2f&color=fff&bold=true',
      observaciones: 'Buen dominio del contenido, excelente clase demostrativa.',
      puntajes: {},
      totalPersonalidad: 0,
      totalClase: 0,
      totalEntrevista: 0
    },
    {
      id: 3,
      nombre: 'JORGE HUMBERTO',
      apellido: 'GUANÍN FAJARDO',
      hora: '02:00 PM',
      modalidad: 'presencial',
      estado: 'pendiente',
      foto: 'https://ui-avatars.com/api/?name=Jorge+Guanin&background=1d4ed8&color=fff&bold=true',
      observaciones: '',
      puntajes: {},
      totalPersonalidad: 0,
      totalClase: 0,
      totalEntrevista: 0
    }
  ];

  // =====================================================
  // RÚBRICA REAL — FCCDD-UTEQ
  // Total 25 puntos: 15 personalidad + 10 clase
  // =====================================================
  criteriosPersonalidad: CriterioEntrevista[] = [
    { id: 'voc_docente',   label: 'Vocación Docente',                  max: 5, grupo: 'personalidad' },
    { id: 'actitud',       label: 'Actitud Asertiva',                  max: 2, grupo: 'personalidad' },
    { id: 'habilidades',   label: 'Habilidades Sociales y Comunicativas', max: 2, grupo: 'personalidad' },
    { id: 'compromiso',    label: 'Compromiso y Responsabilidad',      max: 2, grupo: 'personalidad' },
    { id: 'autenticidad',  label: 'Autenticidad y Autoimagen Realista', max: 2, grupo: 'personalidad' },
    { id: 'flexibilidad',  label: 'Flexibilidad Cognitiva',            max: 2, grupo: 'personalidad' }
  ];

  criteriosClase: CriterioEntrevista[] = [
    { id: 'hab_pedagogicas', label: 'Habilidades Pedagógicas en Acción',              max: 2, grupo: 'clase' },
    { id: 'dominio',         label: 'Dominio del Contenido',                           max: 2, grupo: 'clase' },
    { id: 'improvisacion',   label: 'Capacidad de Improvisación y Manejo de Situaciones', max: 2, grupo: 'clase' },
    { id: 'innovacion',      label: 'Potencial para la Innovación',                   max: 2, grupo: 'clase' },
    { id: 'tecnologias',     label: 'Habilidades de Uso de Tecnologías para la Enseñanza', max: 2, grupo: 'clase' }
  ];

  todosLosCriterios: CriterioEntrevista[] = [];

  // =====================================================
  // UI STATE
  // =====================================================
  candidatoSeleccionado: EntrevistaCandidato | null = null;
  guardando = false;
  mostrarModalConfirm = false;
  mostrarModalAgenda = false;

  // Modal agendar
  nuevaEntrevista = {
    nombre: '',
    fecha: '',
    hora: '',
    modalidad: 'virtual' as 'presencial' | 'virtual',
    enlace: ''
  };

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.todosLosCriterios = [...this.criteriosPersonalidad, ...this.criteriosClase];
    this.inicializarPuntajes();
  }

  // =====================================================
  // INICIALIZAR PUNTAJES
  // =====================================================
  inicializarPuntajes(): void {
    this.candidatos.forEach(c => {
      this.todosLosCriterios.forEach(criterio => {
        if (c.puntajes[criterio.id] === undefined) c.puntajes[criterio.id] = 0;
      });
      this.recalcular(c);
    });
  }

  // =====================================================
  // SELECCIÓN
  // =====================================================
  seleccionar(c: EntrevistaCandidato): void {
    this.candidatoSeleccionado = c;
  }

  // =====================================================
  // CÁLCULO
  // =====================================================
  recalcular(c: EntrevistaCandidato): void {
    c.totalPersonalidad = this.criteriosPersonalidad.reduce(
      (sum, cr) => sum + Math.min(Number(c.puntajes[cr.id] || 0), cr.max), 0
    );
    c.totalClase = this.criteriosClase.reduce(
      (sum, cr) => sum + Math.min(Number(c.puntajes[cr.id] || 0), cr.max), 0
    );
    c.totalEntrevista = c.totalPersonalidad + c.totalClase;
  }

  maxPersonalidad(): number {
    return this.criteriosPersonalidad.reduce((s, c) => s + c.max, 0); // 15
  }

  maxClase(): number {
    return this.criteriosClase.reduce((s, c) => s + c.max, 0); // 10
  }

  porcentaje(valor: number, max: number): number {
    return max > 0 ? Math.round((valor / max) * 100) : 0;
  }

  // =====================================================
  // GUARDAR
  // =====================================================
  guardarEntrevista(): void {
    if (!this.candidatoSeleccionado) return;
    this.guardando = true;
    setTimeout(() => {
      this.candidatoSeleccionado!.estado = 'completado';
      this.guardando = false;
      this.mostrarModalConfirm = true;
    }, 700);
  }

  cerrarModalConfirm(): void {
    this.mostrarModalConfirm = false;
  }

  // =====================================================
  // AGENDAR
  // =====================================================
  abrirModalAgenda(): void {
    this.mostrarModalAgenda = true;
  }

  guardarAgenda(): void {
    this.mostrarModalAgenda = false;
    alert(`Entrevista agendada para ${this.nuevaEntrevista.nombre} el ${this.nuevaEntrevista.fecha} a las ${this.nuevaEntrevista.hora}`);
  }

  // =====================================================
  // UNIRSE A SALA VIRTUAL
  // =====================================================
  unirseEntrevista(c: EntrevistaCandidato): void {
    if (c.enlace) {
      window.open(c.enlace, '_blank');
      c.estado = 'en_curso';
    }
  }

  // =====================================================
  // NAVEGACIÓN
  // =====================================================
  volver(): void {
    this.router.navigate(['/evaluador']);
  }

  // =====================================================
  // HELPERS
  // =====================================================
  estadoLabel(e: string): string {
    const map: { [k: string]: string } = {
      pendiente: 'Pendiente', en_curso: 'En curso', completado: 'Completado'
    };
    return map[e] || e;
  }
}
