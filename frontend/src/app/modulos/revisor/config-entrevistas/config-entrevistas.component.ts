import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

type Vista = 'fases' | 'plantillas' | 'criterios';

interface FaseDTO {
  idFase?: number;
  nombre: string;
  tipo: string;
  peso: number;
  orden: number;
  estado: boolean;
  idPlantilla?: number;
  nombrePlantilla?: string;
  evaluadoresPermitidos: string[];
  expandida?: boolean;
}

interface PlantillaDTO {
  idPlantilla?: number;
  codigo: string;
  nombre: string;
  idFase: number;
  nombreFase?: string;
  numeroCriterios?: number;
  ultimaModificacion?: string;
  estado: boolean;
}

interface CriterioDTO {
  idCriterio?: number;
  nombre: string;
  descripcion: string;
  peso: number;
  escala: string;
  rubrica?: string;
  idPlantilla: number;
}

@Component({
  selector: 'app-config-entrevistas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './config-entrevistas.component.html',
  styleUrls: ['./config-entrevistas.component.scss']
})
export class ConfigEntrevistasComponent implements OnInit {

  private readonly API_FASES      = 'http://localhost:8080/api/evaluacion/fases';
  private readonly API_PLANTILLAS = 'http://localhost:8080/api/evaluacion/plantillas';
  private readonly API_CRITERIOS  = 'http://localhost:8080/api/evaluacion/criterios';

  vistaActual: Vista = 'fases';
  cargando = false;
  guardando = false;
  error = '';

  fases: FaseDTO[] = [];
  showModalFase = false;
  editandoFase = false;
  formFase: Partial<FaseDTO> = {};

  tiposFase = [
    { value: 'automatica', label: 'Automática' },
    { value: 'reunion',    label: 'Reunión' },
    { value: 'practica',   label: 'Práctica' },
    { value: 'decision',   label: 'Decisión' }
  ];

  plantillas: PlantillaDTO[] = [];
  showModalPlantilla = false;
  editandoPlantilla = false;
  formPlantilla: Partial<PlantillaDTO> = {};

  criterios: CriterioDTO[] = [];
  plantillaSeleccionada: PlantillaDTO | null = null;
  showModalCriterio = false;
  editandoCriterio = false;
  formCriterio: Partial<CriterioDTO> = {};

  escalas = [
    { value: '1-5',   label: 'Escala 1-5' },
    { value: '1-10',  label: 'Escala 1-10' },
    { value: '0-100', label: 'Escala 0-100' }
  ];

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargarFases();
  }

  irA(vista: Vista): void {
    this.vistaActual = vista;
    this.error = '';
    if (vista === 'fases')      this.cargarFases();
    if (vista === 'plantillas') this.cargarPlantillas();
  }

  verCriteriosDe(p: PlantillaDTO): void {
    this.plantillaSeleccionada = p;
    this.vistaActual = 'criterios';
    this.cargarCriterios(p.idPlantilla!);
  }

  // ── FASES ─────────────────────────────────────────────────

  cargarFases(): void {
    this.cargando = true;
    this.http.get<FaseDTO[]>(this.API_FASES).subscribe({
      next: (data) => { this.fases = data; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'Error al cargar fases.'; this.cargando = false; }
    });
  }

  abrirCrearFase(): void {
    this.editandoFase = false;
    this.formFase = { nombre: '', tipo: 'reunion', peso: 33, orden: this.fases.length + 1, estado: true, evaluadoresPermitidos: [] };
    this.showModalFase = true;
  }

  abrirEditarFase(f: FaseDTO): void {
    this.editandoFase = true;
    this.formFase = { ...f };
    this.showModalFase = true;
  }

  cerrarModalFase(): void { this.showModalFase = false; this.formFase = {}; }

  guardarFase(): void {
    if (!this.formFase.nombre?.trim()) { alert('El nombre es obligatorio.'); return; }
    if (!this.formFase.peso)           { alert('El peso es obligatorio.'); return; }

    this.guardando = true;
    const req = this.editandoFase
      ? this.http.put(`${this.API_FASES}/${this.formFase.idFase}`, this.formFase)
      : this.http.post(this.API_FASES, this.formFase);

    req.subscribe({
      next: () => { this.guardando = false; this.cerrarModalFase(); this.cargarFases(); },
      error: (err) => { alert(err?.error?.mensaje || 'Error al guardar.'); this.guardando = false; }
    });
  }

  eliminarFase(f: FaseDTO): void {
    if (!confirm(`¿Eliminar la fase "${f.nombre}"?`)) return;
    this.http.delete(`${this.API_FASES}/${f.idFase}`).subscribe({
      next: () => this.cargarFases(),
      error: (err) => alert(err?.error?.mensaje || 'Error al eliminar.')
    });
  }

  cambiarEstadoFase(f: FaseDTO): void {
    this.http.put(`${this.API_FASES}/${f.idFase}`, { ...f, estado: !f.estado }).subscribe({
      next: () => this.cargarFases(),
      error: () => {}
    });
  }

  get pesoTotal(): number { return this.fases.reduce((s, f) => s + (f.peso || 0), 0); }
  get pesoValido(): boolean { return this.pesoTotal === 100; }

  getTipoLabel(tipo: string): string {
    return this.tiposFase.find(t => t.value === tipo)?.label ?? tipo;
  }

  getTipoBadge(tipo: string): string {
    const m: Record<string, string> = { automatica: 'info', reunion: 'success', practica: 'warning', decision: 'purple' };
    return m[tipo] ?? 'default';
  }

  // ── PLANTILLAS ────────────────────────────────────────────

  cargarPlantillas(): void {
    this.cargando = true;
    this.http.get<PlantillaDTO[]>(this.API_PLANTILLAS).subscribe({
      next: (data) => { this.plantillas = data; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'Error al cargar plantillas.'; this.cargando = false; }
    });
  }

  abrirCrearPlantilla(): void {
    this.editandoPlantilla = false;
    this.formPlantilla = { codigo: '', nombre: '', idFase: 0, estado: true };
    this.showModalPlantilla = true;
    if (this.fases.length === 0) this.cargarFases();
  }

  abrirEditarPlantilla(p: PlantillaDTO): void {
    this.editandoPlantilla = true;
    this.formPlantilla = { ...p };
    this.showModalPlantilla = true;
    if (this.fases.length === 0) this.cargarFases();
  }

  cerrarModalPlantilla(): void { this.showModalPlantilla = false; this.formPlantilla = {}; }

  guardarPlantilla(): void {
    if (!this.formPlantilla.codigo?.trim()) { alert('El código es obligatorio.'); return; }
    if (!this.formPlantilla.nombre?.trim()) { alert('El nombre es obligatorio.'); return; }
    if (!this.formPlantilla.idFase)         { alert('Seleccione una fase.'); return; }

    this.guardando = true;
    const req = this.editandoPlantilla
      ? this.http.put(`${this.API_PLANTILLAS}/${this.formPlantilla.idPlantilla}`, this.formPlantilla)
      : this.http.post(this.API_PLANTILLAS, this.formPlantilla);

    req.subscribe({
      next: () => { this.guardando = false; this.cerrarModalPlantilla(); this.cargarPlantillas(); },
      error: (err) => { alert(err?.error?.mensaje || 'Error al guardar.'); this.guardando = false; }
    });
  }

  eliminarPlantilla(p: PlantillaDTO): void {
    if (!confirm(`¿Eliminar la plantilla "${p.nombre}"?`)) return;
    this.http.delete(`${this.API_PLANTILLAS}/${p.idPlantilla}`).subscribe({
      next: () => this.cargarPlantillas(),
      error: (err) => alert(err?.error?.mensaje || 'Error al eliminar.')
    });
  }

  // ── CRITERIOS ─────────────────────────────────────────────

  cargarCriterios(idPlantilla: number): void {
    this.cargando = true;
    this.http.get<CriterioDTO[]>(`${this.API_PLANTILLAS}/${idPlantilla}/criterios`).subscribe({
      next: (data) => { this.criterios = data; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'Error al cargar criterios.'; this.cargando = false; }
    });
  }

  abrirCrearCriterio(): void {
    this.editandoCriterio = false;
    this.formCriterio = { nombre: '', descripcion: '', peso: 33, escala: '1-5', idPlantilla: this.plantillaSeleccionada?.idPlantilla };
    this.showModalCriterio = true;
  }

  abrirEditarCriterio(c: CriterioDTO): void {
    this.editandoCriterio = true;
    this.formCriterio = { ...c };
    this.showModalCriterio = true;
  }

  cerrarModalCriterio(): void { this.showModalCriterio = false; this.formCriterio = {}; }

  guardarCriterio(): void {
    if (!this.formCriterio.nombre?.trim())      { alert('El nombre es obligatorio.'); return; }
    if (!this.formCriterio.descripcion?.trim()) { alert('La descripción es obligatoria.'); return; }
    if (!this.formCriterio.peso)                { alert('El peso es obligatorio.'); return; }

    this.guardando = true;
    const req = this.editandoCriterio
      ? this.http.put(`${this.API_CRITERIOS}/${this.formCriterio.idCriterio}`, this.formCriterio)
      : this.http.post(this.API_CRITERIOS, this.formCriterio);

    req.subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarModalCriterio();
        this.cargarCriterios(this.plantillaSeleccionada!.idPlantilla!);
      },
      error: (err) => { alert(err?.error?.mensaje || 'Error al guardar.'); this.guardando = false; }
    });
  }

  eliminarCriterio(c: CriterioDTO): void {
    if (!confirm(`¿Eliminar el criterio "${c.nombre}"?`)) return;
    this.http.delete(`${this.API_CRITERIOS}/${c.idCriterio}`).subscribe({
      next: () => this.cargarCriterios(this.plantillaSeleccionada!.idPlantilla!),
      error: (err) => alert(err?.error?.mensaje || 'Error al eliminar.')
    });
  }

  get pesoCriteriosTotal(): number { return this.criterios.reduce((s, c) => s + (c.peso || 0), 0); }
  get pesoCriteriosValido(): boolean { return this.pesoCriteriosTotal === 100; }

  getEscalaLabel(e: string): string {
    return this.escalas.find(s => s.value === e)?.label ?? e;
  }
}
