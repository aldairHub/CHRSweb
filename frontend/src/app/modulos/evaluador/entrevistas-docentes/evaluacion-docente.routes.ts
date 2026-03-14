// evaluacion-docente.routes.ts
// Rutas del módulo entrevistas-docentes

import { Routes } from '@angular/router';

export const EVALUACION_DOCENTE_ROUTES: Routes = [
  {
    path: '',
    title: 'SSDC - Entrevistas Docentes',
    loadComponent: () =>
      import('./entrevistas-lista/entrevistas-lista.component')
        .then(m => m.EntrevistasListaComponent)
  },
  {
    path: 'dashboard',
    title: 'SSDC - Dashboard Evaluación',
    loadComponent: () =>
      import('./dashboard/dashboard.component')
        .then(m => m.EvaluacionDashboardComponent)
  },
  {
    path: 'fases',
    loadComponent: () =>
      import('./config-fases/config-fases.component')
        .then(m => m.ConfigFasesComponent)
  },
  {
    path: 'plantillas',
    loadComponent: () =>
      import('./config-plantillas/config-plantillas.component')
        .then(m => m.ConfigPlantillasComponent)
  },
  {
    path: 'criterios',
    loadComponent: () =>
      import('./config-criterios/config-criterios.component')
        .then(m => m.ConfigCriteriosComponent)
  },
  {
    path: 'criterios/:id',
    loadComponent: () =>
      import('./config-criterios/config-criterios.component')
        .then(m => m.ConfigCriteriosComponent)
  },
  {
    path: 'postulantes/:idSolicitud',
    title: 'SSDC - Postulantes',
    loadComponent: () =>
      import('./postulantes/postulantes.component')
        .then(m => m.PostulantesComponent)
  },
  {
    path: 'postulantes',
    loadComponent: () =>
      import('./postulantes/postulantes.component')
        .then(m => m.PostulantesComponent)
  },
  {
    path: 'postulantes/:id',
    loadComponent: () =>
      import('./postulantes/postulantes.component')
        .then(m => m.PostulantesComponent)
  },
  {
    path: 'programar-reunion',
    loadComponent: () =>
      import('./programar-reunion/programar-reunion.component')
        .then(m => m.ProgramarReunionComponent)
  },
  {
    path: 'programar-reunion/:idProceso',
    loadComponent: () =>
      import('./programar-reunion/programar-reunion.component')
        .then(m => m.ProgramarReunionComponent)
  },
  {
    path: 'programar-reunion/:idProceso/:idFase',
    loadComponent: () =>
      import('./programar-reunion/programar-reunion.component')
        .then(m => m.ProgramarReunionComponent)
  },
  {
    path: 'evaluacion',
    redirectTo: 'postulantes',
    pathMatch: 'full'
  },
  {
    path: 'evaluacion/:idReunion',
    loadComponent: () =>
      import('./evaluacion/evaluacion.component')
        .then(m => m.EvaluacionComponent)
  },
  {
    path: 'resultados',
    loadComponent: () =>
      import('./resultados/resultados.component')
        .then(m => m.ResultadosComponent)
  },
  {
    path: 'resultados/:idProceso',
    loadComponent: () =>
      import('./resultados/resultados.component')
        .then(m => m.ResultadosComponent)
  }
];
