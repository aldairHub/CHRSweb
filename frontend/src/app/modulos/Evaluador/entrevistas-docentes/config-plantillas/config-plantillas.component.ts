// entrevistas-docentes/config-plantillas/config-plantillas.component.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../../component/navbar';
import { ConfigPlantillasService } from '../../../../services/entrevistas/config-plantillas.service';
import { FasesService } from '../../../../services/entrevistas/config-fases.service';
import { PlantillaRequest, PlantillaResponse, FaseResponse } from '../../../../models/entrevistas-models';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-config-plantillas',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './config-plantillas.component.html',
  styleUrls: ['./config-plantillas.component.scss']
})
export class ConfigPlantillasComponent implements OnInit {

  plantillas: PlantillaResponse[] = [];
  fasesDisponibles: FaseResponse[] = [];
  isLoading = true;
  error = '';

  showModal = false;
  editMode  = false;
  isSaving  = false;

  form: PlantillaRequest & { idPlantilla?: number } = this.initForm();

  constructor(
    private router: Router,
    private cdr: ChangeDetectorRef,
    private plantillasService: ConfigPlantillasService,
    private fasesService: FasesService
  ) {}

  ngOnInit(): void { this.cargarDatos(); }

  cargarDatos(): void {
    this.isLoading = true;
    this.error = '';
    forkJoin({
      plantillas: this.plantillasService.listar(),
      fases:      this.fasesService.listar()
    }).subscribe({
      next: ({ plantillas, fases }) => {
        this.plantillas      = plantillas;
        this.fasesDisponibles = fases.filter(f => f.estado);
        this.isLoading       = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando datos:', err);
        this.error     = 'No se pudieron cargar los datos.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  initForm(): PlantillaRequest & { idPlantilla?: number } {
    return { codigo: 'PLNT-', nombre: '', idFase: 0, estado: true };
  }

  openCreate(): void {
    this.editMode  = false;
    this.form      = this.initForm();
    this.showModal = true;
  }

  openEdit(p: PlantillaResponse): void {
    this.editMode = true;
    this.form = { idPlantilla: p.idPlantilla, codigo: p.codigo, nombre: p.nombre, idFase: p.idFase, estado: p.estado };
    this.showModal = true;
  }

  closeModal(): void { this.showModal = false; }

  save(): void {
    if (!this.form.codigo?.trim()) { alert('El código es obligatorio.'); return; }
    if (!this.form.nombre?.trim()) { alert('El nombre es obligatorio.'); return; }
    if (!this.form.idFase)         { alert('Selecciona una fase.'); return; }

    this.isSaving = true;
    const payload: PlantillaRequest = {
      codigo: this.form.codigo, nombre: this.form.nombre,
      idFase: Number(this.form.idFase), estado: this.form.estado
    };

    const op$ = this.editMode && this.form.idPlantilla
      ? this.plantillasService.actualizar(this.form.idPlantilla, payload)
      : this.plantillasService.crear(payload);

    op$.subscribe({
      next: () => { this.isSaving = false; this.closeModal(); this.cargarDatos(); },
      error: (err) => {
        alert(err?.error?.message || 'Error al guardar la plantilla.');
        this.isSaving = false;
        this.cdr.detectChanges();
      }
    });
  }

  verCriterios(p: PlantillaResponse): void {
    this.router.navigate(['/entrevistas-docentes/criterios', p.idPlantilla]);
  }
}
