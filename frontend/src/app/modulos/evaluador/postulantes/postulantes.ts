import { Component, OnInit,ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';

export interface PostulanteLista {
  idPostulacion:       number;
  idPostulante:        number;
  identificacion:      string;
  nombresPostulante:   string;
  apellidosPostulante: string;
  correoPostulante:    string;
  estadoPostulacion:   string;
  nombreMateria:       string;
}

@Component({
  selector: 'app-postulantes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './postulantes.html',
  styleUrls: ['./postulantes.scss']
})
export class PostulantesComponent implements OnInit {

  cargando = true;
  filtro = { cedula: '', apellido: '' };
  mostrarModal       = false;
  correoSeleccionado = '';

  postulantes:      PostulanteLista[] = [];
  postulantesTotal: PostulanteLista[] = [];
  errorCarga: string | null = null;

  constructor(private router: Router,private cdr: ChangeDetectorRef, private http: HttpClient) {}

  ngOnInit(): void {
    this.cargarPostulantes();
  }

  cargarPostulantes(): void {
    this.cargando   = true;
    this.errorCarga = null;
    const token   = localStorage.getItem('token');
    this.cdr.detectChanges();
    const headers = new HttpHeaders({ Authorization: 'Bearer ' + token });
    this.cdr.detectChanges();

    this.http.get<PostulanteLista[]>('/api/postulaciones/evaluador/lista', { headers })
      .subscribe({
        next: (data) => {
          this.postulantesTotal = data;
          this.postulantes      = data;
          this.cargando         = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorCarga = 'No se pudo cargar la lista de postulantes.';
          this.cargando   = false;
          this.cdr.detectChanges();
        }
      });
  }

  buscar(): void {
    const cedula   = this.filtro.cedula.toLowerCase().trim();
    const apellido = this.filtro.apellido.toLowerCase().trim();
    this.cdr.detectChanges();
    this.postulantes = this.postulantesTotal.filter(p => {
      this.cdr.detectChanges();
      const matchCedula   = !cedula   || p.identificacion.toLowerCase().includes(cedula);
      const matchApellido = !apellido || p.apellidosPostulante.toLowerCase().includes(apellido);
      return matchCedula && matchApellido;
      this.cdr.detectChanges();
    });
  }

  limpiarFiltros(): void {
    this.filtro = { cedula: '', apellido: '' };
    this.postulantes = this.postulantesTotal;
  }

  verDocumentos(p: PostulanteLista): void {
    this.router.navigate(['/evaluador/documentos/' + p.idPostulacion]);
    this.cdr.detectChanges();
  }

  volver(): void {
    this.router.navigate(['/evaluador']);
    this.cdr.detectChanges();
  }

  abrirModalExito(correo: string = ''): void {
    this.correoSeleccionado = correo;
    this.mostrarModal = true;
    this.cdr.detectChanges();
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.cdr.detectChanges();
  }

  estadoClass(estado: string): string {
    switch ((estado || '').toLowerCase()) {
      case 'pendiente':    return 'status-pendiente';
      case 'en_revision':  return 'status-revision';
      case 'aprobada':     return 'status-aprobado';
      case 'rechazada':    return 'status-rechazado';
      default:             return 'status-pendiente';
    }
  }
}
