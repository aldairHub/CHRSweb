import { Routes } from '@angular/router';
  import { AuthGuard } from './services/auth.guard'; // Ajusta la ruta si es necesario

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
    // RUTAS DEL EVALUADOR (Aquí estaban los errores)
    // ==========================================
    {
      path: 'evaluador',
      canActivate: [AuthGuard],
      data: { rol: 'evaluador' },
      children: [
        {
          // DASHBOARD PRINCIPAL
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
          // OJO: Aquí usamos EvaluacionMeritosComponent (nombre corregido)
          path: 'evaluacion',
          loadComponent: () => import('./modulos/Evaluador/evaluacion/evaluacion')
            .then(m => m.EvaluacionMeritosComponent)
        },
        {
          // CORREGIDO: Antes decía m.Reportes
          path: 'reportes',
          loadComponent: () => import('./modulos/Evaluador/reportes/reportes')
            .then(m => m.ReportesComponent)
        },
        {
          // CORREGIDO: Antes decía m.Entrevistas
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
      path: 'roles-autoridad',
      loadComponent: () => import('./modulos/Admin/roles-autoridad/roles-autoridad')
        .then(m => m.RolesAutoridadComponent),
      canActivate: [AuthGuard],
      data: { rol: 'admin' }
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
    },{
      path: 'registrar-terna',
      loadComponent: () => import('./modulos/estructura/terna/terna')
        .then(m => m.TernaComponent),
      canActivate: [AuthGuard],
      data: { rol: 'admin' }
    },

    // ==========================================
    // ERROR 404 (Cualquier ruta desconocida)
    // ==========================================
    {
      path: '**',
      redirectTo: 'login'
    }
  ];
