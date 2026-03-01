// // src/app/modulos/evaluacion-docente/config-criterios/config-criterios.ts
//
// import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
// import { CommonModule } from '@angular/common';
// import { FormsModule } from '@angular/forms';
// import { ActivatedRoute } from '@angular/router';
// import { NavbarComponent } from '../../../../component/navbar';
// import { FooterComponent } from '../../../../component/footer';
//
// @Component({
//   selector: 'app-config-criterios',
//   standalone: true,
//   imports: [CommonModule, FormsModule, NavbarComponent],
//   templateUrl: './config-criterios.html',
//   styleUrls: ['./config-criterios.scss']
// })
// export class ConfigCriteriosComponent implements OnInit {
//
//   idPlantilla = 1;
//   nombrePlantilla = 'Conocimientos Técnicos (PLNT-TEC-001)';
//
//   criterios: CriterioDTO[] = [
//     { idCriterio: 1, nombre: 'Dominio de la materia', descripcion: 'Evalúa el conocimiento profundo del contenido de la materia', peso: 20, escala: '1-5', idPlantilla: 1 },
//     { idCriterio: 2, nombre: 'Experiencia práctica', descripcion: 'Años de experiencia en el área y casos prácticos', peso: 15, escala: '1-5', idPlantilla: 1 },
//     { idCriterio: 3, nombre: 'Capacidad de análisis', descripcion: 'Resolución de problemas y pensamiento crítico', peso: 20, escala: '1-5', idPlantilla: 1 },
//     { idCriterio: 4, nombre: 'Actualización académica', descripcion: 'Conocimiento de tendencias y nuevas tecnologías', peso: 15, escala: '1-5', idPlantilla: 1 },
//     { idCriterio: 5, nombre: 'Comunicación técnica', descripcion: 'Claridad al explicar conceptos complejos', peso: 15, escala: '1-5', idPlantilla: 1 },
//     { idCriterio: 6, nombre: 'Certificaciones y títulos', descripcion: 'Formación académica y certificaciones relevantes', peso: 15, escala: '1-5', idPlantilla: 1 },
//   ];
//
//   get pesoTotal(): number { return this.criterios.reduce((s, c) => s + c.peso, 0); }
//   get pesoValido(): boolean { return this.pesoTotal === 100; }
//
//   showModal = false;
//   editMode  = false;
//   isSaving  = false;
//
//   form: Partial<CriterioDTO> = this.initForm();
//
//   escalasDisponibles: Array<{ value: '1-5' | '1-10' | '0-100', label: string }> = [
//     { value: '1-5',   label: '1 a 5 estrellas' },
//     { value: '1-10',  label: '1 a 10 puntos' },
//     { value: '0-100', label: '0 a 100 puntos' }
//   ];
//
//   constructor(private route: ActivatedRoute, private cdr: ChangeDetectorRef) {}
//
//   ngOnInit(): void {
//     this.route.params.subscribe(p => {
//       if (p['id']) this.idPlantilla = +p['id'];
//     });
//   }
//
//   initForm(): Partial<CriterioDTO> {
//     return { nombre: '', descripcion: '', peso: 10, escala: '1-5', rubrica: '', idPlantilla: this.idPlantilla };
//   }
//
//   openCreate(): void {
//     this.editMode  = false;
//     this.form      = this.initForm();
//     this.showModal = true;
//     this.cdr.detectChanges();
//   }
//
//   openEdit(c: CriterioDTO): void {
//     this.editMode  = true;
//     this.form      = { ...c };
//     this.showModal = true;
//     this.cdr.detectChanges();
//   }
//
//   closeModal(): void {
//     this.showModal = false;
//     this.cdr.detectChanges();
//   }
//
//   delete(c: CriterioDTO): void {
//     if (!confirm(`¿Eliminar el criterio "${c.nombre}"?`)) return;
//     this.criterios = this.criterios.filter(x => x.idCriterio !== c.idCriterio);
//     this.cdr.detectChanges();
//   }
//
//   save(): void {
//     if (!this.form.nombre?.trim())   { alert('El nombre es obligatorio.'); return; }
//     if (!this.form.descripcion?.trim()) { alert('La descripción es obligatoria.'); return; }
//     if (!this.form.peso || this.form.peso < 1) { alert('El peso debe ser mayor a 0.'); return; }
//
//     this.isSaving = true;
//
//     if (this.editMode) {
//       const idx = this.criterios.findIndex(c => c.idCriterio === this.form.idCriterio);
//       if (idx >= 0) this.criterios[idx] = { ...this.criterios[idx], ...(this.form as CriterioDTO) };
//     } else {
//       this.criterios.push({
//         idCriterio:  Date.now(),
//         nombre:      this.form.nombre!,
//         descripcion: this.form.descripcion!,
//         peso:        this.form.peso!,
//         escala:      this.form.escala!,
//         rubrica:     this.form.rubrica,
//         idPlantilla: this.idPlantilla
//       });
//     }
//
//     this.isSaving = false;
//     this.closeModal();
//     this.cdr.detectChanges();
//   }
//
//   getEscalaLabel(e: string): string {
//     const map: Record<string, string> = { '1-5': '1 – 5', '1-10': '1 – 10', '0-100': '0 – 100' };
//     return map[e] ?? e;
//   }
// }
// entrevistas-docentes/config-criterios/config-criterios.component.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { NavbarComponent } from '../../../../component/navbar';
import { ConfigCriteriosService } from '../../../../services/entrevistas/config-criterios.service';
import { ConfigPlantillasService } from '../../../../services/entrevistas/config-plantillas.service';
import { CriterioRequest, CriterioResponse } from '../../../../models/entrevistas-models';

@Component({
  selector: 'app-config-criterios',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
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
    private cdr: ChangeDetectorRef,
    private criteriosService: ConfigCriteriosService,
    private plantillasService: ConfigPlantillasService
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

  cargarDatos(): void {
    this.isLoading = true;
    this.error = '';

    this.plantillasService.obtener(this.idPlantilla).subscribe({
      next: (p) => { this.nombrePlantilla = `${p.nombre} (${p.codigo})`; this.cdr.detectChanges(); },
      error: (err) => console.error('Error cargando plantilla:', err)
    });

    this.criteriosService.listarPorPlantilla(this.idPlantilla).subscribe({
      next: (data) => { this.criterios = data; this.isLoading = false; this.cdr.detectChanges(); },
      error: (err) => {
        console.error('Error cargando criterios:', err);
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
