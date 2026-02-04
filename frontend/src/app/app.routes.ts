import { Routes } from '@angular/router';
import { AuthGuard } from './services/auth.guard';
import { FacultadComponent } from './modulos/estructura/facultad/facultad';
//cagre
export const routes: Routes = [

  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

  {
    path: 'login',
    loadComponent: () => import('./modulos/login/login').then(m => m.LoginComponent)
  },

  {
    path: 'registro',
    loadComponent: () => import('./modulos/Registro/registro').then(m => m.RegistroComponent)
  },

  {
    path: 'postulante',
    loadComponent: () => import('./modulos/Postulante/postulante').then(m => m.PostulanteComponent),
    canActivate: [AuthGuard],
    data: { rol: 'postulante' }
  },

  {
    path: 'evaluador',
    loadComponent: () => import('./modulos/Evaluador/evaluador').then(m => m.EvaluadorComponent),
    canActivate: [AuthGuard],
    data: { rol: 'evaluador' }
  },

  {
    path: 'admin',
    loadComponent: () => import('./modulos/Admin/admin').then(m => m.AdminComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin' }
  },

  {
    path: 'crear-evaluador',
    loadComponent: () => import('./modulos/crear-auto/crear-auto')
      .then(m => m.CrearEvaluadorComponent)
  },

  {
    path: 'facultad',
    loadComponent: () => import('./modulos/estructura/facultad/facultad')
      .then(m => m.FacultadComponent)
  },

  {
    path: 'carrera',
    loadComponent: () =>
      import('./modulos/estructura/carrera/carrera')
        .then(m => m.CarreraComponent)
  },
  {
    path: 'materias',
    loadComponent: () => import('./modulos/estructura/materia/materia')
      .then(m => m.MateriaComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin' }
  },

  {
    path: 'gestion-usuarios',
    loadComponent: () =>
      import('./modulos/gestion-usuarios/gestion-usuarios')
        .then(m => m.GestionUsuariosComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin' }
  },

  {
    path: '**',
    redirectTo: 'login'
  }

];

