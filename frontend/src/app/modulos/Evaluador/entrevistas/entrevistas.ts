import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar'; // Ajusta ruta
import { FooterComponent } from '../../../component/footer'; // Ajusta ruta

@Component({
  selector: 'app-entrevistas',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, FooterComponent],
  templateUrl: './entrevistas.html',
  styleUrls: ['./entrevistas.scss']
})
export class EntrevistasComponent {

  candidatoSeleccionado: any = null;

  // Lista de candidatos agendados
  candidatos = [
    {
      id: 1,
      nombre: 'HERRERA CASTRO MIGUEL',
      hora: '09:00 AM',
      estado: 'Pendiente',
      foto: 'https://ui-avatars.com/api/?name=Miguel+Herrera&background=random',
      notas: '',
      puntajes: { dominio: 0, didactica: 0, recursos: 0 }
    },
    {
      id: 2,
      nombre: 'RODRIGUEZ VEGA ANDRES',
      hora: '10:30 AM',
      estado: 'Completado',
      foto: 'https://ui-avatars.com/api/?name=Andres+Rodriguez&background=random',
      notas: 'Excelente dominio del tema.',
      puntajes: { dominio: 9, didactica: 8, recursos: 9 }
    },
    {
      id: 3,
      nombre: 'MARTINEZ SILVA CAROLINA',
      hora: '02:00 PM',
      estado: 'Pendiente',
      foto: 'https://ui-avatars.com/api/?name=Carolina+Martinez&background=random',
      notas: '',
      puntajes: { dominio: 0, didactica: 0, recursos: 0 }
    }
  ];

  seleccionar(candidato: any) {
    this.candidatoSeleccionado = candidato;
  }

  guardarEntrevista() {
    if (!this.candidatoSeleccionado) return;

    // Simular guardado
    this.candidatoSeleccionado.estado = 'Completado';
    alert(`Entrevista de ${this.candidatoSeleccionado.nombre} guardada correctamente.`);

    // Opcional: Limpiar selección
    // this.candidatoSeleccionado = null;
  }

  calcularTotal(): number {
    if (!this.candidatoSeleccionado) return 0;
    const p = this.candidatoSeleccionado.puntajes;
    return (p.dominio || 0) + (p.didactica || 0) + (p.recursos || 0);
  }
}
