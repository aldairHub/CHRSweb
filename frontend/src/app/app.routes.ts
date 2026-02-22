import { Routes } from '@angular/router';
import { AuthGuard } from './services/auth.guard';

export const routes: Routes = [

  // ==========================================
  // RUTAS PÚBLICAS
  // ==========================================
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
    path: 'recuperar-clave',
    loadComponent: () =>
      import('./modulos/recuperar-clave/recuperar-clave')
        .then(m => m.RecuperarClaveComponent)
  },
  // ==========================================
  // RUTA SIN ACCESO (rol no mapeado)
  // ==========================================
  {
    path: 'sin-acceso',
    loadComponent: () => import('./modulos/sin-acceso/sin-acceso').then(m => m.SinAccesoComponent)
  },
  {
    path: 'perfil',
    loadComponent: () =>
      import('./modulos/perfil/perfil')
        .then(m => m.PerfilComponent),
    canActivate: [AuthGuard]
  },

  {
    path: 'cambiar-clave-obligatorio',
    loadComponent: () =>
      import('./modulos/cambiar-clave-obligatorio/cambiar-clave-obligatorio')
        .then(m => m.CambiarClaveObligatorioComponent),
    canActivate: [AuthGuard]
  },

// ✅ Alias por si ya lo estabas usando con "cambio-..."
  {
    path: 'cambio-clave-obligatorio',
    redirectTo: 'cambiar-clave-obligatorio',
    pathMatch: 'full'
  },
  // ==========================================
  // RUTA DEL POSTULANTE
  // ==========================================
  {
    path: 'postulante',
    loadComponent: () => import('./modulos/Postulante/postulante').then(m => m.PostulanteComponent),
    canActivate: [AuthGuard],
    data: { rol: 'postulante', isHome: true }
  },

  // ==========================================
  // RUTAS DEL EVALUADOR
  // ==========================================
  {
    path: 'evaluador',
    canActivate: [AuthGuard],
    data: { rol: 'evaluador' },
    children: [
      {
        path: '',
        loadComponent: () => import('./modulos/Evaluador/evaluador').then(m => m.EvaluadorComponent),
        data: { isHome: true }
      },
      {
        path: 'solicitar',
        loadComponent: () => import('./modulos/Evaluador/solicitar-docente/solicitar-docente')
          .then(m => m.SolicitarDocenteComponent)
      },
      {
        path: 'postulantes',
        loadComponent: () => import('./modulos/Evaluador/postulantes/postulantes')
          .then(m => m.PostulantesComponent)
      },
      {
        path: 'documentos',
        loadComponent: () => import('./modulos/Evaluador/documentos/documentos')
          .then(m => m.DocumentosComponent)
      },
      {
        path: 'evaluacion',
        loadComponent: () => import('./modulos/Evaluador/evaluacion/evaluacion')
          .then(m => m.EvaluacionMeritosComponent)
      },
      {
        path: 'reportes',
        loadComponent: () => import('./modulos/Evaluador/reportes/reportes')
          .then(m => m.ReportesComponent)
      },
      {
        path: 'entrevistas',
        loadComponent: () => import('./modulos/Evaluador/entrevistas/entrevistas')
          .then(m => m.EntrevistasComponent)
      }
    ]
  },

  // ==========================================
  // RUTAS DE ADMIN
  // ==========================================
  {
    path: 'admin',
    loadComponent: () => import('./modulos/Admin/admin').then(m => m.AdminComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin', isHome: true }
  },
  {
    path: 'gestion-roles',
    loadComponent: () => import('./modulos/Admin/gestion-roles/gestion-roles')
      .then(m => m.GestionRolesComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin' }
  },
  {
    path: 'roles-autoridad',
    redirectTo: 'gestion-roles',
    pathMatch: 'full'
  },
  {
    path: 'gestion-usuarios',
    loadComponent: () => import('./modulos/gestion-usuarios/gestion-usuarios')
      .then(m => m.GestionUsuariosComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin' }
  },
  {
    path: 'facultad',
    loadComponent: () => import('./modulos/estructura/facultad/facultad').then(m => m.FacultadComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin' }
  },
  {
    path: 'carrera',
    loadComponent: () => import('./modulos/estructura/carrera/carrera')
      .then(m => m.CarreraComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin' }
  },
  {
    path: 'materia',
    loadComponent: () => import('./modulos/estructura/materia/materia')
      .then(m => m.MateriaComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin' }
  },
  {
    path: 'gestion-postulante',
    loadComponent: () => import('./modulos/estructura/postulante/postulante')
      .then(m => m.PostulanteComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin' }
  },
  {
    path: 'gestion-documentos',
    loadComponent: () => import('./modulos/Admin/gestiondocumentos/gestion-documentos')
      .then(m => m.GestionDocumentosComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin' }
  },

  // ==========================================
  // ✅ RUTAS DEL REVISOR (Vicerrectorado)
  // ==========================================
  {
    path: 'revisor',
    canActivate: [AuthGuard],
    data: { rol: 'revisor' },
    children: [
      {
        path: '',
        loadComponent: () => import('./modulos/vicerrectorado/vicerrectorado')
          .then(m => m.VicerrectoradoComponent),
        data: { isHome: true }
      }//,
      // {
      //   path: 'prepostulaciones',
      //   loadComponent: () => import('./modulos/vicerrectorado/prepostulaciones/prepostulaciones')
      //     .then(m => m.PrepostulacionesRevisorComponent)
      // },
      // {
      //   path: 'solicitudes',
      //   loadComponent: () => import('./modulos/vicerrectorado/solicitudes/solicitudes')
      //     .then(m => m.SolicitudesRevisorComponent)
      // }
    ]
  },

  // ==========================================
  // ERROR 404
  // ==========================================
  {
    path: '**',
    redirectTo: 'login'
  }
];
