// entrevistas-docentes/config-fases/config-fases.component.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../../component/navbar';
import { FasesService } from '../../../../services/entrevistas/config-fases.service';
import { FaseRequest, FaseResponse } from '../../../../models/entrevistas-models';

@Component({
  selector: 'app-config-fases',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './config-fases.component.html',
  styleUrls: ['./config-fases.component.scss']
})
export class ConfigFasesComponent implements OnInit {

  fases: FaseResponse[] = [];
  isLoading = true;
  error = '';

  get pesoTotal(): number { return this.fases.reduce((s, f) => s + f.peso, 0); }
  get pesoValido(): boolean { return this.pesoTotal === 100; }

  showModal = false;
  editMode  = false;
  isSaving  = false;

  form: FaseRequest & { idFase?: number } = this.initForm();

  tiposDisponibles = [
    { value: 'automatica', label: 'Automática (Revisión Documental)' },
    { value: 'reunion',    label: 'Reunión de Evaluación' },
    { value: 'practica',  label: 'Actividad Práctica' },
    { value: 'decision',  label: 'Decisión de Comité' }
  ];

  evaluadoresOpciones = [
    'Comité de Selección', 'Coordinador', 'Experto Técnico',
    'Decano', 'Pedagogo', 'Comité Evaluador', 'Vicerrectorado'
  ];

  constructor(
    private cdr: ChangeDetectorRef,
    private fasesService: FasesService
  ) {}

  ngOnInit(): void { this.cargarFases(); }

  cargarFases(): void {
    this.isLoading = true;
    this.error = '';
    this.fasesService.listar().subscribe({
      next: (data) => {
        this.fases     = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando fases:', err);
        this.error     = 'No se pudieron cargar las fases.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  initForm(): FaseRequest & { idFase?: number } {
    return { nombre: '', tipo: 'reunion', peso: 10, orden: 1, evaluadoresPermitidos: [], estado: true };
  }

  openCreate(): void {
    this.editMode  = false;
    this.form      = this.initForm();
    this.form.orden = this.fases.length + 1;
    this.showModal = true;
  }

  openEdit(f: FaseResponse): void {
    this.editMode  = true;
    this.form = {
      idFase: f.idFase, nombre: f.nombre, tipo: f.tipo,
      peso: f.peso, orden: f.orden,
      evaluadoresPermitidos: [...f.evaluadoresPermitidos], estado: f.estado
    };
    this.showModal = true;
  }

  closeModal(): void { this.showModal = false; }

  toggleEvaluador(ev: string): void {
    const arr = this.form.evaluadoresPermitidos;
    const idx = arr.indexOf(ev);
    if (idx >= 0) arr.splice(idx, 1); else arr.push(ev);
    this.form.evaluadoresPermitidos = [...arr];
  }

  isEvaluadorSelected(ev: string): boolean {
    return this.form.evaluadoresPermitidos.includes(ev);
  }

  save(): void {
    if (!this.form.nombre?.trim()) { alert('El nombre es obligatorio.'); return; }
    if (!this.form.peso || this.form.peso < 1) { alert('El peso debe ser mayor a 0.'); return; }

    this.isSaving = true;
    const payload: FaseRequest = {
      nombre: this.form.nombre, tipo: this.form.tipo,
      peso: this.form.peso, orden: this.form.orden,
      evaluadoresPermitidos: this.form.evaluadoresPermitidos, estado: this.form.estado
    };

    const op$ = this.editMode && this.form.idFase
      ? this.fasesService.actualizar(this.form.idFase, payload)
      : this.fasesService.crear(payload);

    op$.subscribe({
      next: () => { this.isSaving = false; this.closeModal(); this.cargarFases(); },
      error: (err) => {
        console.error('Error guardando fase:', err);
        alert('Error al guardar la fase.');
        this.isSaving = false;
        this.cdr.detectChanges();
      }
    });
  }

  cambiarEstado(f: FaseResponse): void {
    this.fasesService.cambiarEstado(f.idFase, !f.estado).subscribe({
      next: () => this.cargarFases(),
      error: () => alert('Error al cambiar el estado.')
    });
  }

  getTipoBadge(tipo: string): string {
    const map: Record<string, string> = {
      automatica: 'info', reunion: 'warning', practica: 'pending', decision: 'danger'
    };
    return map[tipo] ?? 'default';
  }

  getTipoLabel(tipo: string): string {
    const map: Record<string, string> = {
      automatica: 'Automática', reunion: 'Reunión', practica: 'Práctica', decision: 'Decisión'
    };
    return map[tipo] ?? tipo;
  }
}
