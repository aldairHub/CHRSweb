import { Routes } from '@angular/router';
import { AuthGuard } from './services/auth.guard';
import {NoPrimerLoginGuard} from './services/primer-login.guard';

export const routes: Routes = [

  // ── Landing (raíz) ────────────────────────────────────────────────────────
  {
    path: '',
    title: 'SSDC',
    loadComponent: () => import('./modulos/landing/landing').then(m => m.LandingComponent),
    pathMatch: 'full'
  },

  // ── Convocatorias públicas ────────────────────────────────────────────────
  {
    path: 'convocatorias',title: 'SSDC - Convocatorias',
    loadComponent: () => import('./modulos/convocatorias-publicas/convocatorias-publicas')
      .then(m => m.ConvocatoriasPublicasComponent)
  },

  // ── Re-postulación ────────────────────────────────────────────────────────
  {
    path: 'repostulacion',
    title: 'SSDC - Repostulación',
    loadComponent: () => import('./modulos/repostulacion/repostulacion')
      .then(m => m.RepostulacionComponent)
  },

  // ── Públicas (sin cambios) ────────────────────────────────────────────────
  {
    path: 'login', title: 'SSDC - Inicio de sesión',
    loadComponent: () => import('./modulos/login/login').then(m => m.LoginComponent)
  },
  {
    path: 'registro',
    title: 'SSDC - Registrar prepostulación',
    loadComponent: () => import('./modulos/././registro/registro').then(m => m.RegistroComponent)
  },
  {
    path: 'recuperar-clave',title: 'SSDC - Recuperar clave',
    loadComponent: () => import('./modulos/recuperar-clave/recuperar-clave')
      .then(m => m.RecuperarClaveComponent)
  },

  // ── Sin acceso ────────────────────────────────────────────────────────────
  { path: 'sin-acceso',title: 'SSDC - SIN ACCESO',
    loadComponent: () =>
      import('./modulos/sin-acceso/sin-acceso').then(m => m.SinAccesoComponent) },

  // ── Perfil / cambio de clave ──────────────────────────────────────────────
  { path: 'perfil', title: 'SSDC - Perfil',
    loadComponent: () =>
      import('./modulos/perfil/perfil').then(m => m.PerfilComponent),
    canActivate: [AuthGuard] },
  { path: 'cambiar-clave-obligatorio',
    title: 'SSDC - Cambio de clave',
    loadComponent: () =>
      import('./modulos/cambiar-clave-obligatorio/cambiar-clave-obligatorio')
        .then(m => m.CambiarClaveObligatorioComponent), canActivate: [AuthGuard] },
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
        title: 'SSDC - Subir documentos',
        loadComponent: () => import('./modulos/././postulante/subir-documentos/subir-documentos')
          .then(m => m.SubirDocumentosComponent)
      },
      {
        path: 'resultados',
        title: 'SSDC - Resultados',
        loadComponent: () => import('./modulos/././postulante/resultados/resultados')
          .then(m => m.ResultadosComponent)
      },
      {
        path: 'entrevistas',
        title: 'SSDC - Entrevistas',
        loadComponent: () => import('./modulos/././postulante/entrevista/entrevista')
          .then(m => m.EntrevistaPostulanteComponent)
      }
    ]
  },
  // ── evaluador ─────────────────────────────────────────────────────────────
  { path: 'evaluador',
    title: 'SSDC - Evaluador',canActivate: [AuthGuard], data: { rol: 'evaluador' }, children: [
      { path: '', loadComponent: () =>
          import('./modulos/././evaluador/evaluador').then(m => m.EvaluadorComponent),
        data: { isHome: true } },
      { path: 'solicitar',
        title: 'SSDC - Solicitud de docente',loadComponent: () =>
          import('./modulos/././evaluador/solicitar-docente/solicitar-docente')
            .then(m => m.SolicitarDocenteComponent) },
      { path: 'postulantes',
        title: 'SSDC - Postulantes',loadComponent: () =>
          import('./modulos/././evaluador/postulantes/postulantes').then(m => m.PostulantesComponent) },
      { path: 'documentos',
        title: 'SSDC - Documentos',loadComponent: () =>
          import('./modulos/././evaluador/documentos/documentos').then(m => m.DocumentosComponent) },
      // Reemplaza la ruta actual de matriz-meritos por estas dos:
      { path: 'matriz-meritos',
        title: 'SSDC - Matriz de Méritos',
        loadComponent: () =>
          import('./modulos/evaluador/matriz-meritos-lista/matriz-meritos-lista.component')
            .then(m => m.MatrizMeritosListaComponent) },
      { path: 'matriz-meritos/:idConvocatoria',
        title: 'SSDC - Matriz de Méritos',
        loadComponent: () =>
          import('./modulos/evaluador/matriz-meritos/matriz-meritos.component')
            .then(m => m.MatrizMeritosComponent) },
      { path: 'reportes',
        title: 'SSDC - Reportes',loadComponent: () =>
          import('./modulos/././evaluador/reportes/reportes').then(m => m.ReportesComponent) },
      {
        path: 'entrevistas-docentes',
        title: 'SSDC - Entrevistas',
        loadChildren: () =>
          import('./modulos/././evaluador/entrevistas-docentes/evaluacion-docente.routes')
            .then(m => m.EVALUACION_DOCENTE_ROUTES)
      }
    ]},

  // ── admin ─────────────────────────────────────────────────────────────────
  {
    path: 'admin',
    canActivate: [AuthGuard],
    data: { rol: 'admin' },
    children: [
      {
        path: '',
        title: 'SSDC - Administrador',
        loadComponent: () => import('./modulos/admin/admin').then(m => m.AdminComponent),
        data: { isHome: true }
      },
      {
        path: 'gestion-roles',
        title: 'SSDC - Roles',
        loadComponent: () => import('./modulos/admin/gestion-roles/gestion-roles').then(m => m.GestionRolesComponent)
      },
      {
        path: 'gestion-usuarios',
        title: 'SSDC - Usuarios',
        loadComponent: () => import('./modulos/admin/gestion-usuarios/gestion-usuarios').then(m => m.GestionUsuariosComponent)
      },
      {
        path: 'facultad',
        title: 'SSDC - Facultades',
        loadComponent: () => import('./modulos/admin/facultad/facultad').then(m => m.FacultadComponent)
      },
      {
        path: 'carrera',
        title: 'SSDC - Carreras',
        loadComponent: () => import('./modulos/admin/carrera/carrera').then(m => m.CarreraComponent)
      },
      {
        path: 'materia',
        title: 'SSDC - Materias',
        loadComponent: () => import('./modulos/admin/materia/materia').then(m => m.MateriaComponent)
      },
      {
        path: 'gestion-postulante',
        title: 'SSDC - Postulantes',
        loadComponent: () => import('./modulos/admin/postulante/postulante').then(m => m.PostulanteComponent)
      },
      {
        path: 'gestion-documentos',
        title: 'SSDC - Gestión de documentos',
        loadComponent: () => import('./modulos/admin/gestiondocumentos/gestion-documentos').then(m => m.GestionDocumentosComponent)
      },
      {
        path: 'auditoria',
        title: 'SSDC - Auditoría',
        canActivate: [NoPrimerLoginGuard],
        loadComponent: () => import('./modulos/admin/auditoria/auditoria').then(m => m.AuditoriaComponent)
      },
      {
        path: 'auditoria/sesiones',
        title: 'SSDC - Sesiones activas',
        canActivate: [NoPrimerLoginGuard],
        loadComponent: () => import('./modulos/admin/auditoria/sesiones-activas/sesiones-activas').then(m => m.SesionesActivasComponent)
      },
      {
        path: 'niveles-academicos',
        title: 'SSDC - Niveles Académicos',
        loadComponent: () => import('./modulos/admin/gestion-niveles/gestion-niveles')
          .then(m => m.GestionNivelesComponent)
      },
      {
        path: 'auditoria/historial',
        title: 'SSDC - Historial de acciones',
        canActivate: [NoPrimerLoginGuard],
        loadComponent: () => import('./modulos/admin/auditoria/historial-acciones/historial-acciones').then(m => m.HistorialAccionesComponent)
      },
      {
        path: 'auditoria/estadisticas',
        title: 'SSDC - Estadísticas de auditoría',
        canActivate: [NoPrimerLoginGuard],
        loadComponent: () => import('./modulos/admin/auditoria/estadisticas/estadisticas').then(m => m.EstadisticasAuditoriaComponent)
      },
      {
        path: 'gestion-opciones',
        title: 'SSDC - Configuración de opciones',
        loadComponent: () => import('./modulos/admin/gestion-opciones/gestion-opciones').then(m => m.GestionOpcionesComponent)
      },
      {
        path: 'config-institucion',
        title: 'SSDC - Institución',
        loadComponent: () => import('./modulos/admin/config-institucion/config-institucion').then(m => m.ConfigInstitucionComponent)
      },
      // Alias de compatibilidad
      { path: 'roles-autoridad', redirectTo: 'gestion-roles', pathMatch: 'full' },
    ]
  },


  // ── Revisor (Vicerrectorado) ──────────────────────────────────────────────
  { path: 'revisor',
    title: 'SSDC - Revisor', canActivate: [AuthGuard], data: { rol: 'revisor' }, children: [
      { path: '', loadComponent: () =>
          import('./modulos/revisor/revisor').then(m => m.RevisorComponent),
        data: { isHome: true } },
      { path: 'convocatorias',
        title: 'SSDC - Convocatorias',loadComponent: () =>
          import('./modulos/revisor/convocatoria/convocatoria').then(m => m.ConvocatoriaComponent) },
      { path: 'solicitudes-docente',
        title: 'SSDC - Solicitudes',loadComponent: () =>
          import('./modulos/revisor/solicitudesdocentes/solicitudes-docente')
            .then(m => m.SolicitudesDocenteComponent) },
      { path: 'prepostulaciones',
        title: 'SSDC - Prepostulaciones',loadComponent: () =>
          import('./modulos/revisor/gestionpostulante/gestionpostulante')
            .then(m => m.GestionPostulanteComponent) },
    ]},
  // ── Historial notificaciones (todos los roles) ──────────────────────────
  {
    path: 'notificaciones',
    title: 'SSDC - Notificaciones',
    canActivate: [AuthGuard],
    loadComponent: () => import('./modulos/notificaciones/historial-notificaciones')
      .then(m => m.HistorialNotificacionesComponent)
  },

  // ── 404 ───────────────────────────────────────────────────────────────────
  { path: '**', redirectTo: '' }
];
