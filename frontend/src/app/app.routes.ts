import { Routes } from '@angular/router';
import { AuthGuard } from './services/auth.guard';
import { NoPrimerLoginGuard } from './services/primer-login.guard';

export const routes: Routes = [

  // ── Landing (raíz) ────────────────────────────────────────────────────────
  {
    path: '',
    loadComponent: () => import('./modulos/landing/landing').then(m => m.LandingComponent),
    pathMatch: 'full'
  },

  // ── Convocatorias públicas ────────────────────────────────────────────────
  {
    path: 'convocatorias',
    loadComponent: () => import('./modulos/convocatorias-publicas/convocatorias-publicas')
      .then(m => m.ConvocatoriasPublicasComponent)
  },

  // ── Re-postulación ────────────────────────────────────────────────────────
  {
    path: 'repostulacion',
    loadComponent: () => import('./modulos/repostulacion/repostulacion')
      .then(m => m.RepostulacionComponent)
  },

  // ── Públicas ──────────────────────────────────────────────────────────────
  {
    path: 'login',
    loadComponent: () => import('./modulos/login/login').then(m => m.LoginComponent)
  },
  {
    path: 'registro',
    loadComponent: () => import('./modulos/././registro/registro').then(m => m.RegistroComponent)
  },
  {
    path: 'recuperar-clave',
    loadComponent: () => import('./modulos/recuperar-clave/recuperar-clave')
      .then(m => m.RecuperarClaveComponent)
  },

  // ── Sin acceso ────────────────────────────────────────────────────────────
  {
    path: 'sin-acceso',
    loadComponent: () =>
      import('./modulos/sin-acceso/sin-acceso').then(m => m.SinAccesoComponent)
  },

  // ── Perfil / cambio de clave ──────────────────────────────────────────────
  {
    path: 'perfil',
    loadComponent: () =>
      import('./modulos/perfil/perfil').then(m => m.PerfilComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'cambiar-clave-obligatorio',
    loadComponent: () =>
      import('./modulos/cambiar-clave-obligatorio/cambiar-clave-obligatorio')
        .then(m => m.CambiarClaveObligatorioComponent),
    canActivate: [AuthGuard]
  },
  { path: 'cambio-clave-obligatorio', redirectTo: 'cambiar-clave-obligatorio', pathMatch: 'full' },

  // ── postulante ────────────────────────────────────────────────────────────
  {
    path: 'postulante',
    canActivate: [AuthGuard, NoPrimerLoginGuard],
    data: { rol: 'postulante' },
    children: [
      {
        path: '',
        loadComponent: () => import('./modulos/././postulante/postulante').then(m => m.PostulanteComponent),
        data: { isHome: true }
      },
      {
        path: 'subir-documentos',
        loadComponent: () => import('./modulos/././postulante/subir-documentos/subir-documentos')
          .then(m => m.SubirDocumentosComponent)
      },
      {
        path: 'resultados',
        loadComponent: () => import('./modulos/././postulante/resultados/resultados')
          .then(m => m.ResultadosComponent)
      },
      {
        path: 'entrevistas',
        loadComponent: () => import('./modulos/././postulante/entrevista/entrevista')
          .then(m => m.EntrevistaPostulanteComponent)
      }
    ]
  },

  // ── evaluador ─────────────────────────────────────────────────────────────
  {
    path: 'evaluador',
    canActivate: [AuthGuard],
    data: { rol: 'evaluador' },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./modulos/././evaluador/evaluador').then(m => m.EvaluadorComponent),
        data: { isHome: true }
      },
      {
        path: 'solicitar',
        loadComponent: () =>
          import('./modulos/././evaluador/solicitar-docente/solicitar-docente')
            .then(m => m.SolicitarDocenteComponent)
      },
      {
        path: 'postulantes',
        loadComponent: () =>
          import('./modulos/././evaluador/postulantes/postulantes').then(m => m.PostulantesComponent)
      },
      // ── Documentos con y sin ID ──────────────────────────────────────────
      {
        path: 'documentos/:id',
        loadComponent: () =>
          import('./modulos/././evaluador/documentos/documentos').then(m => m.DocumentosComponent)
      },
      { path: 'documentos', redirectTo: 'postulantes', pathMatch: 'full' },
      // ── Evaluación de méritos ────────────────────────────────────────────
      {
        path: 'evaluacion',
        loadComponent: () =>
          import('./modulos/././evaluador/evaluacion/evaluacion').then(m => m.EvaluacionMeritosComponent)
      },
      // ── Matriz de méritos ────────────────────────────────────────────────
      {
        path: 'matriz-meritos',
        loadComponent: () =>
          import('./modulos/evaluador/matriz-meritos-lista/matriz-meritos-lista.component')
            .then(m => m.MatrizMeritosListaComponent)
      },
      { path: 'matriz-meritos/:idSolicitud',
        loadComponent: () =>
          import('./modulos/evaluador/matriz-meritos/matriz-meritos.component')
            .then(m => m.MatrizMeritosComponent) },
      {
        path: 'reportes',
        loadComponent: () =>
          import('./modulos/././evaluador/reportes/reportes').then(m => m.ReportesComponent)
      },
      {
        path: 'entrevistas-docentes',
        loadChildren: () =>
          import('./modulos/././evaluador/entrevistas-docentes/evaluacion-docente.routes')
            .then(m => m.EVALUACION_DOCENTE_ROUTES)
      }
    ]
  },

  // ── admin ─────────────────────────────────────────────────────────────────
  {
    path: 'admin',
    canActivate: [AuthGuard],
    data: { rol: 'admin' },
    children: [
      {
        path: '',
        loadComponent: () => import('./modulos/admin/admin').then(m => m.AdminComponent),
        data: { isHome: true }
      },
      {
        path: 'gestion-roles',
        loadComponent: () => import('./modulos/admin/gestion-roles/gestion-roles').then(m => m.GestionRolesComponent)
      },
      {
        path: 'gestion-usuarios',
        loadComponent: () => import('./modulos/admin/gestion-usuarios/gestion-usuarios').then(m => m.GestionUsuariosComponent)
      },
      {
        path: 'facultad',
        loadComponent: () => import('./modulos/admin/facultad/facultad').then(m => m.FacultadComponent)
      },
      {
        path: 'carrera',
        loadComponent: () => import('./modulos/admin/carrera/carrera').then(m => m.CarreraComponent)
      },
      {
        path: 'materia',
        loadComponent: () => import('./modulos/admin/materia/materia').then(m => m.MateriaComponent)
      },
      {
        path: 'gestion-postulante',
        loadComponent: () => import('./modulos/admin/postulante/postulante').then(m => m.PostulanteComponent)
      },
      {
        path: 'gestion-documentos',
        loadComponent: () => import('./modulos/admin/gestiondocumentos/gestion-documentos').then(m => m.GestionDocumentosComponent)
      },
      {
        path: 'auditoria',
        canActivate: [NoPrimerLoginGuard],
        loadComponent: () => import('./modulos/admin/auditoria/auditoria').then(m => m.AuditoriaComponent)
      },
      {
        path: 'auditoria/sesiones',
        canActivate: [NoPrimerLoginGuard],
        loadComponent: () => import('./modulos/admin/auditoria/sesiones-activas/sesiones-activas').then(m => m.SesionesActivasComponent)
      },
      {
        path: 'niveles-academicos',
        loadComponent: () => import('./modulos/admin/gestion-niveles/gestion-niveles')
          .then(m => m.GestionNivelesComponent)
      },
      {
        path: 'auditoria/historial',
        canActivate: [NoPrimerLoginGuard],
        loadComponent: () => import('./modulos/admin/auditoria/historial-acciones/historial-acciones').then(m => m.HistorialAccionesComponent)
      },
      {
        path: 'auditoria/estadisticas',
        canActivate: [NoPrimerLoginGuard],
        loadComponent: () => import('./modulos/admin/auditoria/estadisticas/estadisticas').then(m => m.EstadisticasAuditoriaComponent)
      },

      {
        path: 'gestion-opciones',
        loadComponent: () => import('./modulos/admin/gestion-opciones/gestion-opciones').then(m => m.GestionOpcionesComponent)
      },
      {
        path: 'config-institucion',
        loadComponent: () => import('./modulos/admin/config-institucion/config-institucion').then(m => m.ConfigInstitucionComponent)
      },
      {
        path: 'backup',
        loadComponent: () => import('./modulos/admin/backup/backup')
          .then(m => m.BackupComponent)
      },
      // Alias de compatibilidad
      { path: 'roles-autoridad', redirectTo: 'gestion-roles', pathMatch: 'full' },
    ]
  },

  // ── Revisor (Vicerrectorado) ──────────────────────────────────────────────
  {
    path: 'revisor',
    canActivate: [AuthGuard],
    data: { rol: 'revisor' },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./modulos/revisor/revisor').then(m => m.RevisorComponent),
        data: { isHome: true }
      },
      {
        path: 'convocatorias',
        loadComponent: () =>
          import('./modulos/revisor/convocatoria/convocatoria').then(m => m.ConvocatoriaComponent)
      },
      {
        path: 'solicitudes-docente',
        loadComponent: () =>
          import('./modulos/revisor/solicitudesdocentes/solicitudes-docente')
            .then(m => m.SolicitudesDocenteComponent)
      },
      {
        path: 'prepostulaciones',
        loadComponent: () =>
          import('./modulos/revisor/gestionpostulante/gestionpostulante')
            .then(m => m.GestionPostulanteComponent)
      },
    ]
  },

  // ── Historial notificaciones (todos los roles) ────────────────────────────
  {
    path: 'notificaciones',
    canActivate: [AuthGuard],
    loadComponent: () => import('./modulos/notificaciones/historial-notificaciones')
      .then(m => m.HistorialNotificacionesComponent)
  },

  // ── 404 ───────────────────────────────────────────────────────────────────
  { path: '**', redirectTo: '' }
];
