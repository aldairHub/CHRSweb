import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterModule } from '@angular/router';
import { ConfigCriteriosService } from '../../../../services/entrevistas/config-criterios.service';
import { ConfigPlantillasService } from '../../../../services/entrevistas/config-plantillas.service';
import { EntrevistasEstadoService } from '../../../../services/entrevistas/entrevistas-estado.service';
import { CriterioRequest, CriterioResponse } from '../../../../models/entrevistas-models';

@Component({
  selector: 'app-config-criterios',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './config-criterios.component.html',
  styleUrls: ['./config-criterios.component.scss']
})
export class ConfigCriteriosComponent implements OnInit {

  idPlantilla = 0;
  nombrePlantilla = '';
  criterios: CriterioResponse[] = [];
  isLoading = true;
  error = '';

  get pesoTotal(): number { return this.criterios.reduce((s, c) => s + c.peso, 0); }
  get pesoValido(): boolean { return this.pesoTotal === 100; }

  showModal = false;
  editMode  = false;
  isSaving  = false;

  form: CriterioRequest & { idCriterio?: number } = this.initForm();

  escalasDisponibles = [
    { value: '1-5',   label: '1 a 5 estrellas' },
    { value: '1-10',  label: '1 a 10 puntos' },
    { value: '0-100', label: '0 a 100 puntos' }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private criteriosService: ConfigCriteriosService,
    private plantillasService: ConfigPlantillasService,
    private estado: EntrevistasEstadoService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(p => {
      if (p['id']) {
        this.idPlantilla = +p['id'];
        this.cargarDatos();
      } else {
        this.isLoading = false;
      }
    });
  }

  navegarPostulantes(): void {
    const id = this.estado.getIdSolicitud();
    if (id) {
      this.router.navigate(['/evaluador/entrevistas-docentes/postulantes', id]);
    } else {
      this.router.navigate(['/evaluador/entrevistas-docentes/postulantes']);
    }
  }

  esRutaActiva(segmento: string): boolean {
    return this.router.url.includes(segmento);
  }

  cargarDatos(): void {
    this.isLoading = true;
    this.error = '';

    this.plantillasService.obtener(this.idPlantilla).subscribe({
      next: (p) => { this.nombrePlantilla = `${p.nombre} (${p.codigo})`; this.cdr.detectChanges(); },
      error: () => {}
    });

    this.criteriosService.listarPorPlantilla(this.idPlantilla).subscribe({
      next: (data) => { this.criterios = data; this.isLoading = false; this.cdr.detectChanges(); },
      error: () => {
        this.error = 'No se pudieron cargar los criterios.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  initForm(): CriterioRequest & { idCriterio?: number } {
    return { nombre: '', descripcion: '', peso: 10, escala: '1-5', rubrica: '', idPlantilla: this.idPlantilla };
  }

  openCreate(): void {
    this.editMode  = false;
    this.form      = { ...this.initForm(), idPlantilla: this.idPlantilla };
    this.showModal = true;
  }

  openEdit(c: CriterioResponse): void {
    this.editMode = true;
    this.form = { idCriterio: c.idCriterio, nombre: c.nombre, descripcion: c.descripcion,
      peso: c.peso, escala: c.escala, rubrica: c.rubrica, idPlantilla: c.idPlantilla };
    this.showModal = true;
  }

  closeModal(): void { this.showModal = false; }

  delete(c: CriterioResponse): void {
    if (!confirm(`¿Eliminar el criterio "${c.nombre}"?`)) return;
    this.criteriosService.eliminar(c.idCriterio).subscribe({
      next: () => this.cargarDatos(),
      error: () => alert('Error al eliminar el criterio.')
    });
  }

  save(): void {
    if (!this.form.nombre?.trim())      { alert('El nombre es obligatorio.'); return; }
    if (!this.form.descripcion?.trim()) { alert('La descripción es obligatoria.'); return; }
    if (!this.form.peso || this.form.peso < 1) { alert('El peso debe ser mayor a 0.'); return; }

    this.isSaving = true;
    const payload: CriterioRequest = {
      nombre: this.form.nombre, descripcion: this.form.descripcion,
      peso: this.form.peso, escala: this.form.escala,
      rubrica: this.form.rubrica, idPlantilla: this.idPlantilla
    };

    const op$ = this.editMode && this.form.idCriterio
      ? this.criteriosService.actualizar(this.form.idCriterio, payload)
      : this.criteriosService.crear(payload);

    op$.subscribe({
      next: () => { this.isSaving = false; this.closeModal(); this.cargarDatos(); },
      error: () => { alert('Error al guardar el criterio.'); this.isSaving = false; this.cdr.detectChanges(); }
    });
  }

  getEscalaLabel(e: string): string {
    const map: Record<string, string> = { '1-5': '1 – 5', '1-10': '1 – 10', '0-100': '0 – 100' };
    return map[e] ?? e;
  }
}
