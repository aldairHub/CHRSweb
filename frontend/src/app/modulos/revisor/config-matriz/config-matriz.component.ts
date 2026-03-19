// ══════════════════════════════════════════════════════════════
// config-matriz.component.ts — Módulo Revisor
// ══════════════════════════════════════════════════════════════
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface ItemDTO {
  idItem?: number;
  codigo: string;
  label: string;
  puntajeMaximo: number;
  puntosPor?: string;
  orden: number;
  bloqueado: boolean;
}

interface SeccionDTO {
  idSeccion?: number;
  codigo: string;
  titulo: string;
  descripcion: string;
  puntajeMaximo: number;
  orden: number;
  tipo: string;
  bloqueado: boolean;
  items: ItemDTO[];
  expandida?: boolean;
}

@Component({
  selector: 'app-config-matriz',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './config-matriz.component.html',
  styleUrls: ['./config-matriz.component.scss']
})
export class ConfigMatrizComponent implements OnInit {

  private readonly API = 'http://localhost:8080/api/matriz-config';

  cargando = false;
  guardando = false;
  error = '';

  secciones: SeccionDTO[] = [];

  // Modal sección
  showModalSeccion = false;
  editandoSeccion = false;
  formSeccion: Partial<SeccionDTO> = {};

  // Modal ítem
  showModalItem = false;
  editandoItem = false;
  seccionSeleccionada: SeccionDTO | null = null;
  formItem: Partial<ItemDTO> = {};

  tiposSecciones = [
    { value: 'meritos',     label: 'Méritos' },
    { value: 'experiencia', label: 'Experiencia' }
  ];

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargarEstructura();
  }

  cargarEstructura(): void {
    this.cargando = true;
    this.http.get<any>(`${this.API}/estructura`).subscribe({
      next: (data) => {
        this.secciones = (data.secciones || []).map((s: any) => ({
          ...s,
          expandida: false
        }));
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Error al cargar la estructura de la matriz.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Secciones ─────────────────────────────────────────────
  abrirCrearSeccion(): void {
    this.editandoSeccion = false;
    this.formSeccion = {
      codigo: '',
      titulo: '',
      descripcion: '',
      puntajeMaximo: 10,
      orden: this.secciones.length + 1,
      tipo: 'meritos'
    };
    this.showModalSeccion = true;
  }

  abrirEditarSeccion(s: SeccionDTO): void {
    this.editandoSeccion = true;
    this.formSeccion = { ...s };
    this.showModalSeccion = true;
  }

  cerrarModalSeccion(): void {
    this.showModalSeccion = false;
    this.formSeccion = {};
  }

  guardarSeccion(): void {
    if (!this.formSeccion.titulo?.trim()) { alert('El título es obligatorio.'); return; }
    if (!this.formSeccion.puntajeMaximo) { alert('El puntaje máximo es obligatorio.'); return; }

    this.guardando = true;
    this.http.post(`${this.API}/seccion`, this.formSeccion).subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarModalSeccion();
        this.cargarEstructura();
      },
      error: (err) => {
        alert(err?.error?.mensaje || 'Error al guardar la sección.');
        this.guardando = false;
      }
    });
  }

  eliminarSeccion(s: SeccionDTO): void {
    if (s.bloqueado) { alert('Esta sección no puede eliminarse.'); return; }
    if (!confirm(`¿Eliminar la sección "${s.titulo}"?`)) return;

    this.http.delete(`${this.API}/seccion/${s.idSeccion}`).subscribe({
      next: () => this.cargarEstructura(),
      error: (err) => alert(err?.error?.mensaje || 'Error al eliminar.')
    });
  }

  // ── Ítems ─────────────────────────────────────────────────
  abrirCrearItem(seccion: SeccionDTO): void {
    this.editandoItem = false;
    this.seccionSeleccionada = seccion;
    this.formItem = {
      codigo: '',
      label: '',
      puntajeMaximo: 5,
      puntosPor: '',
      orden: (seccion.items?.length || 0) + 1
    };
    this.showModalItem = true;
  }

  abrirEditarItem(seccion: SeccionDTO, item: ItemDTO): void {
    this.editandoItem = true;
    this.seccionSeleccionada = seccion;
    this.formItem = { ...item };
    this.showModalItem = true;
  }

  cerrarModalItem(): void {
    this.showModalItem = false;
    this.formItem = {};
    this.seccionSeleccionada = null;
  }

  guardarItem(): void {
    if (!this.formItem.label?.trim())    { alert('El label es obligatorio.'); return; }
    if (!this.formItem.puntajeMaximo)    { alert('El puntaje máximo es obligatorio.'); return; }
    if (!this.formItem.codigo?.trim())   { alert('El código es obligatorio.'); return; }

    this.guardando = true;
    const payload = {
      ...this.formItem,
      idSeccion: this.seccionSeleccionada?.idSeccion
    };

    this.http.post(`${this.API}/item`, payload).subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarModalItem();
        this.cargarEstructura();
      },
      error: (err) => {
        alert(err?.error?.mensaje || 'Error al guardar el ítem.');
        this.guardando = false;
      }
    });
  }

  eliminarItem(item: ItemDTO): void {
    if (item.bloqueado) { alert('Este ítem no puede eliminarse.'); return; }
    if (!confirm(`¿Eliminar el ítem "${item.label}"?`)) return;

    this.http.delete(`${this.API}/item/${item.idItem}`).subscribe({
      next: () => this.cargarEstructura(),
      error: (err) => alert(err?.error?.mensaje || 'Error al eliminar.')
    });
  }

  toggleExpandir(s: SeccionDTO): void {
    s.expandida = !s.expandida;
  }

  getTipoLabel(tipo: string): string {
    return { meritos: 'Méritos', experiencia: 'Experiencia', entrevista: 'Entrevista' }[tipo] ?? tipo;
  }

  getTipoBadge(tipo: string): string {
    return { meritos: 'success', experiencia: 'warning', entrevista: 'info' }[tipo] ?? 'default';
  }
}
