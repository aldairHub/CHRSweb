import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar'; // Ajusta ruta
import { FooterComponent } from '../../../component/footer'; // Ajusta ruta

@Component({
  selector: 'app-evaluacion-meritos',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, FooterComponent],
  templateUrl: './evaluacion.html',
  styleUrls: ['./evaluacion.scss']
})
export class EvaluacionMeritosComponent implements OnInit {

  // Simulación de candidatos (Columnas)
  candidatos = [
    { id: 1, nombre: 'HERRERA CASTRO MIGUEL', cargo: 'Ingeniero en Informática', puntajes: {} as any, total: 0 },
    { id: 2, nombre: 'RODRIGUEZ VEGA ANDRES', cargo: 'Ingeniero en Sistemas', puntajes: {} as any, total: 0 },
    { id: 3, nombre: 'MARTINEZ SILVA CAROLINA', cargo: 'Ingeniera en Sistemas', puntajes: {} as any, total: 0 }
  ];

  // Estructura de la Matriz (Filas)
  rubrica = [
    {
      titulo: 'A) Título de cuarto nivel',
      descripcion: 'Maestría vinculada al campo amplio de la asignatura',
      maximo: 20,
      items: [
        { id: 'a1', label: 'Título verificado', max: 20 }
      ]
    },
    {
      titulo: 'B) Experiencia docencia/investigación',
      descripcion: 'Experiencia universitaria o politécnica',
      maximo: 10,
      items: [
        { id: 'b1', label: 'Docencia universitaria (1 punto por año)', max: 5 },
        { id: 'b2', label: 'Proyectos de investigación (1 punto por proyecto)', max: 3 },
        { id: 'b3', label: 'Gestión académica (1 punto por año)', max: 2 }
      ]
    },
    {
      titulo: 'C) Publicaciones',
      descripcion: 'En el área afín del concurso',
      maximo: 6,
      items: [
        { id: 'c1', label: 'Libros (2 puntos por libro)', max: 4 },
        { id: 'c2', label: 'Artículos en revistas indexadas (2 puntos)', max: 4 }
      ]
    },
    // Puedes agregar D, E, etc. aquí siguiendo el mismo formato
  ];

  constructor() {}

  ngOnInit(): void {
    this.inicializarPuntajes();
  }

  inicializarPuntajes() {
    // Inicializa los valores en 0 para evitar errores
    this.candidatos.forEach(c => {
      this.rubrica.forEach(seccion => {
        seccion.items.forEach(item => {
          c.puntajes[item.id] = 0;
        });
      });
      this.calcularTotal(c);
    });
  }

  // Se ejecuta cada vez que cambias un input
  calcularTotal(candidato: any) {
    let suma = 0;

    // Sumar todos los valores del objeto puntajes
    for (const key in candidato.puntajes) {
      suma += (candidato.puntajes[key] || 0);
    }

    candidato.total = suma;
  }

  guardarEvaluacion() {
    console.log('Guardando evaluación...', this.candidatos);
    alert('Evaluación guardada exitosamente');
  }
}
