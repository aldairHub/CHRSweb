import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token     = localStorage.getItem('token');
  const router    = inject(Router);
  const authState = inject(AuthStateService);

  const rutasPublicas = ['/api/auth/login','/api/registro','/api/auth/recuperar','/api/convocatorias/activas','/api/instituciones/activa'];
  const esPublica = rutasPublicas.some(r => req.url.includes(r));

  const authReq = (token && !esPublica)
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  // Endpoints de datos propios del postulante: un 401/403 aquí
  // no debe expulsar al usuario — el componente los maneja de forma local.
  const ENDPOINTS_DATOS_PROPIOS = [
    '/documentos/postulante',
    '/documentos/resultados',
    '/reuniones/mi-entrevista',
    '/dashboard/postulante',
  ];
  const esDatoPropio = ENDPOINTS_DATOS_PROPIOS.some(u => req.url.includes(u));

  return next(authReq).pipe(
    catchError(err => {
      if (err.status === 401) {
        const esLogout  = req.url.includes('/api/auth/logout');
        const yaEnLogin = router.url.startsWith('/login');
        // Solo redirigir al login si NO es un endpoint de datos propios del postulante
        if (!esLogout && !yaEnLogin && !esDatoPropio) {
          authState.limpiar();
          localStorage.clear();
          router.navigate(['/login'], {
            replaceUrl: true,
            queryParams: { sesionExpirada: 'true' }
          });
        }
      }
      // 403 solo redirige si no es dato propio (evita /sin-acceso injustificado)
      if (err.status === 403 && !esDatoPropio) router.navigate(['/sin-acceso']);
      if (err.status === 500) return throwError(() => err);
      return throwError(() => err);
    })
  );
};
