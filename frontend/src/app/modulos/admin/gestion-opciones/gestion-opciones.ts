import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NavbarComponent } from '../../../component/navbar';

@Component({
  selector: 'app-gestion-opciones',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './gestion-opciones.html',
  styleUrls: ['./gestion-opciones.scss']
})
export class GestionOpcionesComponent implements OnInit {

  // ─── Datos ──────────────────────────────────────────────────────
  roles:    any[] = [];
  opciones: any[] = [];

  // ─── Estado ─────────────────────────────────────────────────────
  rolSeleccionado:  number | null = null;
  nombreRolActual:  string        = '';
  moduloDelRol:     string        = '';
  cargandoOpciones: boolean       = false;
  guardando:        boolean       = false;

  private readonly apiRoles   = 'http://localhost:8080/api/roles-app';
  private readonly apiOpciones= 'http://localhost:8080/api/admin/opciones';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarRoles();
  }

  // ─── Carga roles ────────────────────────────────────────────────
  cargarRoles(): void {
    this.http.get<any[]>(this.apiRoles).subscribe({
      next: data => {
        // Solo roles con módulo asignado — sin módulo no tiene opciones
        this.roles = (Array.isArray(data) ? data : [])
          .filter(r => r.activo);
        this.cdr.detectChanges();
      },
      error: err => console.error('Error cargando roles:', err)
    });
  }

  // ─── Seleccionar rol → carga opciones de su módulo ───────────────
  seleccionarRol(rol: any): void {
    this.rolSeleccionado  = rol.idRolApp;
    this.nombreRolActual  = rol.nombre;
    this.moduloDelRol     = rol.nombreModulo ?? 'Sin módulo';
    this.opciones         = [];
    this.cargandoOpciones = true;
    this.cdr.detectChanges();

    this.http.get<any[]>(`${this.apiOpciones}/rol/${rol.idRolApp}`).subscribe({
      next: data => {
        this.opciones         = Array.isArray(data) ? data : [];
        this.cargandoOpciones = false;
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('Error cargando opciones:', err);
        this.cargandoOpciones = false;
        alert('No se pudieron cargar las opciones. ' +
          'Verifica que el rol tiene un módulo asignado.');
        this.cdr.detectChanges();
      }
    });
  }

  // ─── Toggle habilitada ───────────────────────────────────────────
  toggleOpcion(op: any): void {
    if (op.asignada) {
      // Quitar
      this.http.delete(`${this.apiOpciones}/quitar`, {
        body: { idRolApp: this.rolSeleccionado, idOpcion: op.id_opcion }
      }).subscribe({
        next: () => {
          op.asignada     = false;
          op.solo_lectura = false;
          this.cdr.detectChanges();
        },
        error: err => {
          console.error('Error quitando opción:', err);
          alert('No se pudo quitar la opción.');
        }
      });
    } else {
      // Asignar
      this.http.post(`${this.apiOpciones}/asignar`, {
        idRolApp:    this.rolSeleccionado,
        idOpcion:    op.id_opcion,
        soloLectura: false
      }).subscribe({
        next: () => {
          op.asignada = true;
          this.cdr.detectChanges();
        },
        error: err => {
          console.error('Error asignando opción:', err);
          alert('No se pudo asignar la opción.');
        }
      });
    }
  }

  // ─── Toggle solo lectura ─────────────────────────────────────────
  toggleSoloLectura(op: any): void {
    if (!op.asignada) return; // No tiene sentido si no está habilitada
    const nuevoValor = !op.solo_lectura;

    this.http.post(`${this.apiOpciones}/asignar`, {
      idRolApp:    this.rolSeleccionado,
      idOpcion:    op.id_opcion,
      soloLectura: nuevoValor
    }).subscribe({
      next: () => {
        op.solo_lectura = nuevoValor;
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('Error actualizando solo_lectura:', err);
        alert('No se pudo actualizar el permiso.');
      }
    });
  }
}

