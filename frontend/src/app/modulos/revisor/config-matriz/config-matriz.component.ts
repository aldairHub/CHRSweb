import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';

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
  confirmandoDistribucion = false;
  error = '';

  secciones: SeccionDTO[] = [];
  tieneProcesosActivos = false;

  Math = Math;

  // Distribución editable
  distribucion = { meritos: 50, experiencia: 25, entrevista: 25 };
  distribucionOriginal = { meritos: 50, experiencia: 25, entrevista: 25 };
  distribucionModificada = false;

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
    this.cargarTodo();
  }

  cargarTodo(): void {
    this.cargando = true;

    forkJoin({
      estructura: this.http.get<any>(`${this.API}/estructura`),
      procesos:   this.http.get<any>(`${this.API}/tiene-procesos-activos`)
    }).subscribe({
      next: ({ estructura, procesos }) => {
        this.tieneProcesosActivos = procesos.tieneProcesosActivos;
        this.secciones = (estructura.secciones || []).map((s: any) => ({
          ...s, expandida: false
        }));
        this.leerDistribucionActual();
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Error al cargar la configuración.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  // Lee la distribución actual desde las secciones cargadas
  private leerDistribucionActual(): void {
    let meritos = 0, experiencia = 0, entrevista = 0;

    this.secciones.forEach(s => {
      if (s.tipo === 'meritos')     meritos     += s.puntajeMaximo;
      if (s.tipo === 'experiencia') experiencia += s.puntajeMaximo;
      if (s.tipo === 'entrevista')  entrevista  += s.puntajeMaximo;
    });

    // Si hay secciones de ese tipo, usar su suma; si no, mantener el default
    if (meritos > 0)     this.distribucion.meritos     = meritos;
    if (experiencia > 0) this.distribucion.experiencia = experiencia;
    if (entrevista > 0)  this.distribucion.entrevista  = entrevista;

    this.distribucionOriginal = { ...this.distribucion };
    this.distribucionModificada = false;
  }

  onDistribucionChange(): void {
    this.distribucionModificada =
      this.distribucion.meritos     !== this.distribucionOriginal.meritos     ||
      this.distribucion.experiencia !== this.distribucionOriginal.experiencia ||
      this.distribucion.entrevista  !== this.distribucionOriginal.entrevista;
  }

  get totalDistribucion(): number {
    return (this.distribucion.meritos || 0) +
      (this.distribucion.experiencia || 0) +
      (this.distribucion.entrevista || 0);
  }

  get distribucionValida(): boolean {
    return this.totalDistribucion === 100;
  }

  confirmarDistribucion(): void {
    if (!this.distribucionValida) return;
    if (!confirm(
      `¿Confirmar la nueva distribución de puntajes?\n\n` +
      `• Méritos: ${this.distribucion.meritos} pts\n` +
      `• Experiencia: ${this.distribucion.experiencia} pts\n` +
      `• Entrevista: ${this.distribucion.entrevista} pts\n\n` +
      `Total: ${this.totalDistribucion} / 100`
    )) return;

    this.confirmandoDistribucion = true;

    this.http.post(`${this.API}/confirmar-distribucion`, this.distribucion).subscribe({
      next: () => {
        this.confirmandoDistribucion = false;
        this.distribucionModificada = false;
        this.distribucionOriginal = { ...this.distribucion };
        this.cargarTodo();
      },
      error: (err) => {
        alert(err?.error?.mensaje || 'Error al confirmar la distribución.');
        this.confirmandoDistribucion = false;
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
    if (!this.formSeccion.titulo?.trim())    { alert('El título es obligatorio.'); return; }
    if (!this.formSeccion.puntajeMaximo)     { alert('El puntaje máximo es obligatorio.'); return; }

    this.guardando = true;
    this.http.post(`${this.API}/seccion`, this.formSeccion).subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarModalSeccion();
        this.cargarTodo();
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
      next: () => this.cargarTodo(),
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
    if (!this.formItem.label?.trim())  { alert('El label es obligatorio.'); return; }
    if (!this.formItem.puntajeMaximo)  { alert('El puntaje máximo es obligatorio.'); return; }
    if (!this.formItem.codigo?.trim()) { alert('El código es obligatorio.'); return; }

    this.guardando = true;
    const payload = { ...this.formItem, idSeccion: this.seccionSeleccionada?.idSeccion };

    this.http.post(`${this.API}/item`, payload).subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarModalItem();
        this.cargarTodo();
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
      next: () => this.cargarTodo(),
      error: (err) => alert(err?.error?.mensaje || 'Error al eliminar.')
    });
  }

  toggleExpandir(s: SeccionDTO): void { s.expandida = !s.expandida; }

  getTipoLabel(tipo: string): string {
    return { meritos: 'Méritos', experiencia: 'Experiencia', entrevista: 'Entrevista' }[tipo] ?? tipo;
  }

  getTipoBadge(tipo: string): string {
    return { meritos: 'success', experiencia: 'warning', entrevista: 'info' }[tipo] ?? 'default';
  }
}
