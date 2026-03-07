import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../../../component/navbar';
import { ToastComponent } from '../../../../component/toast.component';
import { ToastService } from '../../../../services/toast.service';

interface HistorialAccion {
  idHistorial: number;
  fecha: string;
  titulo: string;
  descripcion: string | null;
  usuario: string | null;
  proceso?: { idProceso: number };
}

interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Component({
  selector: 'app-historial-acciones',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, RouterLink, ToastComponent],
  templateUrl: './historial-acciones.html',
  styleUrls: ['./historial-acciones.scss']
})
export class HistorialAccionesComponent implements OnInit {

  private readonly apiUrl = 'http://localhost:8080/api/admin/auditoria';

  registros: HistorialAccion[] = [];
  totalElements = 0;
  totalPages    = 0;
  currentPage   = 0;
  pageSize      = 20;
  isLoading     = false;

  filtroUsuario = '';
  filtroDesde   = '';
  filtroHasta   = '';

  detalle: HistorialAccion | null = null;

  constructor(
    private http:  HttpClient,
    private cdr:   ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void { this.cargar(); }

  cargar(page = 0): void {
    this.isLoading = true;
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', this.pageSize.toString());

    if (this.filtroUsuario) params = params.set('usuario', this.filtroUsuario);
    if (this.filtroDesde)   params = params.set('desde',   this.filtroDesde);
    if (this.filtroHasta)   params = params.set('hasta',   this.filtroHasta);

    this.http.get<Page<HistorialAccion>>(`${this.apiUrl}/historial`, { params }).subscribe({
      next: (data) => {
        this.registros     = data.content;
        this.totalElements = data.totalElements;
        this.totalPages    = data.totalPages;
        this.currentPage   = data.number;
        this.isLoading     = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.toast.error('Error', 'No se pudo cargar el historial de acciones.');
        this.cdr.detectChanges();
      }
    });
  }

  aplicarFiltros(): void { this.cargar(0); }

  limpiarFiltros(): void {
    this.filtroUsuario = '';
    this.filtroDesde   = '';
    this.filtroHasta   = '';
    this.cargar(0);
    this.toast.info('Filtros limpiados');
  }

  irPagina(p: number): void {
    if (p >= 0 && p < this.totalPages) this.cargar(p);
  }

  verDetalle(r: HistorialAccion): void  { this.detalle = r; }
  cerrarDetalle(): void                 { this.detalle = null; }

  formatFecha(fecha: string): string {
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  get pages(): number[] {
    const cur = this.currentPage;
    const range: number[] = [];
    for (let i = Math.max(0, cur - 2); i <= Math.min(this.totalPages - 1, cur + 2); i++) {
      range.push(i);
    }
    return range;
  }
}
