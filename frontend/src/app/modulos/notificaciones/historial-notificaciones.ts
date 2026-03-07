import { Component, OnInit, ChangeDetectorRef} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { NavbarComponent } from '../../component/navbar';
import { NotificacionService, Notificacion } from '../../services/notificacion.service';

@Component({
  selector: 'app-historial-notificaciones',
  standalone: true,
  templateUrl: './historial-notificaciones.html',
  styleUrls: ['./historial-notificaciones.scss'],
  imports: [CommonModule, FormsModule, NavbarComponent]
})
export class HistorialNotificacionesComponent implements OnInit {

  // ── Datos ────────────────────────────────────────────────────
  todas: Notificacion[]          = [];
  filtradas: Notificacion[]      = [];
  paginadas: Notificacion[]      = [];

  // ── Filtros ──────────────────────────────────────────────────
  filtroEstado: 'todas' | 'leidas' | 'no_leidas' = 'todas';
  filtroTipo:   string = 'todos';
  busqueda:     string = '';

  // ── Paginación ───────────────────────────────────────────────
  paginaActual  = 1;
  itemsPorPagina = 15;
  totalPaginas  = 1;

  // ── Estado ───────────────────────────────────────────────────
  cargando = true;

  constructor(
    private http: HttpClient,
    private router: Router,private cdr: ChangeDetectorRef,
    public notifService: NotificacionService
  ) {}

  ngOnInit(): void {
    this.cargarTodas();
    this.cdr.detectChanges();
  }

  // ── Carga todas desde el backend (leídas + no leídas) ───────
  cargarTodas(): void {
    this.cargando = true;
    const token = localStorage.getItem('token') ?? '';
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
    this.cdr.detectChanges();
    this.http.get<any>('/api/notificaciones/historial', { headers })
      .pipe(catchError(() => {
        // Si no existe el endpoint de historial, usa el resumen normal
        return this.http.get<any>('/api/notificaciones', { headers });
        this.cdr.detectChanges();
      }))
      .subscribe(data => {
        this.todas = data.notificaciones ?? [];
        this.aplicarFiltros();
        this.cargando = false;
        this.cdr.detectChanges();
      });
  }

  // ── Filtros ──────────────────────────────────────────────────
  aplicarFiltros(): void {
    let resultado = [...this.todas];

    if (this.filtroEstado === 'leidas')    resultado = resultado.filter(n => n.leida);
    if (this.filtroEstado === 'no_leidas') resultado = resultado.filter(n => !n.leida);
    if (this.filtroTipo !== 'todos')       resultado = resultado.filter(n => n.tipo === this.filtroTipo);
    this.cdr.detectChanges();
    if (this.busqueda.trim()) {
      const q = this.busqueda.toLowerCase();
      resultado = resultado.filter(n =>
        n.titulo.toLowerCase().includes(q) ||
        (n.mensaje ?? '').toLowerCase().includes(q)
      );
    }
    this.cdr.detectChanges();
    this.filtradas    = resultado;
    this.totalPaginas = Math.max(1, Math.ceil(resultado.length / this.itemsPorPagina));
    this.paginaActual = 1;
    this.paginar();
    this.cdr.detectChanges();
  }

  paginar(): void {
    const inicio  = (this.paginaActual - 1) * this.itemsPorPagina;
    this.paginadas = this.filtradas.slice(inicio, inicio + this.itemsPorPagina);
    this.cdr.detectChanges();
  }

  paginaAnterior(): void {
    if (this.paginaActual > 1) { this.paginaActual--; this.paginar(); }
    this.cdr.detectChanges();
  }

  paginaSiguiente(): void {
    if (this.paginaActual < this.totalPaginas) { this.paginaActual++; this.paginar(); }
    this.cdr.detectChanges();
  }

  // ── Acciones ─────────────────────────────────────────────────
  marcarLeida(notif: Notificacion): void {
    if (notif.leida) return;
    this.notifService.marcarLeida(notif.idNotificacion);
    notif.leida = true;
    this.cdr.detectChanges();
  }

  marcarTodasLeidas(): void {
    this.notifService.marcarTodasLeidas();
    this.todas = this.todas.map(n => ({ ...n, leida: true }));
    this.aplicarFiltros();
    this.cdr.detectChanges();
  }

  // ── Navegación al módulo ─────────────────────────────────────
  irAModulo(notif: Notificacion): void {
    this.marcarLeida(notif);
    const rol = localStorage.getItem('rol') ?? '';

    switch (notif.entidadTipo) {
      case 'PREPOSTULACION':
        if (rol === 'revisor')        this.router.navigate(['/revisor/prepostulaciones']);
        else if (rol === 'admin')     this.router.navigate(['/admin/gestion-postulante']);
        else                          this.router.navigate(['/' + rol]);
        break;
      case 'SOLICITUD':
        if (rol === 'revisor')        this.router.navigate(['/revisor/solicitudes-docente']);
        else if (rol === 'evaluador') this.router.navigate(['/evaluador/solicitar']);
        else                          this.router.navigate(['/' + rol]);
        break;
      case 'REUNION':
        if (rol === 'evaluador')       this.router.navigate(['/evaluador/entrevistas-docentes']);
        else if (rol === 'postulante') this.router.navigate(['/postulante/entrevistas']);
        else                           this.router.navigate(['/' + rol]);
        break;
      case 'PROCESO':
        if (rol === 'postulante')      this.router.navigate(['/postulante/resultados']);
        else if (rol === 'evaluador')  this.router.navigate(['/evaluador/postulantes']);
        else                           this.router.navigate(['/' + rol]);
        break;
      case 'USUARIO':
        if (rol === 'admin')           this.router.navigate(['/admin/gestion-usuarios']);
        else                           this.router.navigate(['/' + rol]);
        break;
      default:
        this.router.navigate(['/' + rol]);
        this.cdr.detectChanges();
    }
  }

  // ── Helpers ──────────────────────────────────────────────────
  get noLeidasCount(): number {
    return this.todas.filter(n => !n.leida).length;
    this.cdr.detectChanges();
  }

  iconoPorTipo(tipo: string): string {
    switch (tipo) {
      case 'success': return '✓';
      case 'error':   return '✕';
      case 'warning': return '!';
      default:        return 'i';
    }
  }
}
