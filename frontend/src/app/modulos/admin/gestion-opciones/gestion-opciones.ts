import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-gestion-opciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gestion-opciones.html',
  styleUrls: ['./gestion-opciones.scss']
})
export class GestionOpcionesComponent implements OnInit {

  cargando          = false;
  roles:    any[]   = [];
  opciones: any[]   = [];

  rolSeleccionado:  number | null = null;
  nombreRolActual:  string        = '';
  moduloDelRol:     string        = '';
  cargandoOpciones  = false;

  private readonly apiRoles    = '/api/roles-app';
  private readonly apiOpciones = '/api/admin/opciones';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.cargarRoles();
  }

  cargarRoles(): void {
    this.http.get<any[]>(this.apiRoles).subscribe({
      next: data => {
        this.roles = (Array.isArray(data) ? data : []).filter(r => r.activo);
        this.cdr.detectChanges();
      },
      error: () => this.toast.error('No se pudieron cargar los roles.')
    });
  }

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
      error: () => {
        this.cargandoOpciones = false;
        this.toast.error('Verifica que el rol tiene un módulo asignado.');
        this.cdr.detectChanges();
      }
    });
  }

  toggleOpcion(op: any): void {
    if (op.asignada) {
      this.http.delete(`${this.apiOpciones}/quitar`, {
        body: { idRolApp: this.rolSeleccionado, idOpcion: op.id_opcion }
      }).subscribe({
        next: () => { op.asignada = false; this.cdr.detectChanges(); },
        error: () => this.toast.error('No se pudo quitar la opción.')
      });
    } else {
      this.http.post(`${this.apiOpciones}/asignar`, {
        idRolApp:    this.rolSeleccionado,
        idOpcion:    op.id_opcion,
        soloLectura: false
      }).subscribe({
        next: () => { op.asignada = true; this.cdr.detectChanges(); },
        error: () => this.toast.error('No se pudo asignar la opción.')
      });
    }
  }
}
