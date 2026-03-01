// entrevistas-docentes/programar-reunion/programar-reunion.component.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../../../component/navbar';
import { ProgramarReunionService } from '../../../../services/entrevistas/programar-reunion.service';
import { PostulantesService } from '../../../../services/entrevistas/postulantes.service';
import { FasesService } from '../../../../services/entrevistas/config-fases.service';
import { ReunionRequest, PostulanteResumen, FaseResponse } from '../../../../models/entrevistas-models';
import { forkJoin } from 'rxjs';

interface EvaluadorUI {
  id: number;
  nombre: string;
  rol: string;
  seleccionado: boolean;
}

@Component({
  selector: 'app-programar-reunion',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './programar-reunion.component.html',
  styleUrls: ['./programar-reunion.component.scss']
})
export class ProgramarReunionComponent implements OnInit {

  isLoading = true;
  isSaving  = false;
  saved     = false;
  error     = '';

  postulantes: PostulanteResumen[] = [];
  fases: FaseResponse[] = [];

  // Los evaluadores se cargarán desde el backend cuando implementes
  // GET /api/usuarios?rol=evaluador
  // Por ahora se deja vacío para que el admin los gestione manualmente
  evaluadores: EvaluadorUI[] = [];

  duraciones = [
    { value: 30,  label: '30 minutos' },
    { value: 60,  label: '1 hora' },
    { value: 90,  label: '1.5 horas' },
    { value: 120, label: '2 horas' }
  ];

  modalidades = [
    { value: 'zoom',       label: 'Zoom' },
    { value: 'meet',       label: 'Google Meet' },
    { value: 'teams',      label: 'Microsoft Teams' },
    { value: 'presencial', label: 'Presencial' }
  ];

  form = {
    idProceso:    0,
    idFase:       0,
    fecha:        '',
    hora:         '',
    duracion:     60,
    modalidad:    'zoom',
    enlace:       '',
    observaciones: ''
  };

  get evaluadoresSeleccionados(): EvaluadorUI[] { return this.evaluadores.filter(e => e.seleccionado); }
  get showEnlace(): boolean { return this.form.modalidad !== 'presencial'; }

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private reunionService: ProgramarReunionService,
    private postulantesService: PostulantesService,
    private fasesService: FasesService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['idProceso']) this.form.idProceso = +params['idProceso'];
      if (params['idFase'])    this.form.idFase    = +params['idFase'];
    });
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.isLoading = true;
    forkJoin({
      postulantes: this.postulantesService.listar(),
      fases:       this.fasesService.listar()
    }).subscribe({
      next: ({ postulantes, fases }) => {
        this.postulantes = postulantes;
        this.fases       = fases.filter(f => f.estado && f.tipo !== 'automatica' && f.tipo !== 'decision');
        this.isLoading   = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err); this.error = 'No se pudieron cargar los datos.';
        this.isLoading = false; this.cdr.detectChanges();
      }
    });
  }

  toggleEvaluador(ev: EvaluadorUI): void { ev.seleccionado = !ev.seleccionado; }

  save(): void {
    if (!this.form.idProceso)  { alert('Selecciona un postulante.'); return; }
    if (!this.form.idFase)     { alert('Selecciona una fase.'); return; }
    if (!this.form.fecha)      { alert('La fecha es obligatoria.'); return; }
    if (!this.form.hora)       { alert('La hora es obligatoria.'); return; }

    this.isSaving = true;

    const payload: ReunionRequest = {
      idProceso:       this.form.idProceso,
      idFase:          this.form.idFase,
      fecha:           this.form.fecha,
      hora:            this.form.hora,
      duracionMinutos: this.form.duracion,
      modalidad:       this.form.modalidad as any,
      enlace:          this.showEnlace && this.form.enlace ? this.form.enlace : undefined,
      evaluadoresIds:  this.evaluadoresSeleccionados.map(e => e.id),
      observaciones:   this.form.observaciones || undefined
    };

    this.reunionService.programar(payload).subscribe({
      next: () => {
        this.isSaving = false; this.saved = true; this.cdr.detectChanges();
        setTimeout(() => this.router.navigate(['/entrevistas-docentes/postulantes', this.form.idProceso]), 1500);
      },
      error: (err) => {
        alert(err?.error?.mensaje || 'Error al programar la reunión.');
        this.isSaving = false; this.cdr.detectChanges();
      }
    });
  }

  cancelar(): void {
    if (this.form.idProceso) this.router.navigate(['/entrevistas-docentes/postulantes', this.form.idProceso]);
    else this.router.navigate(['/entrevistas-docentes/postulantes']);
  }
}
