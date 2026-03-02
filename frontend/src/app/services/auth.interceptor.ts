import { inject } from "@angular/core";
import { HttpInterceptorFn } from "@angular/common/http";
import { catchError, throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem("token");
  const authReq = token
    ? req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    })
    : req;

  return next(authReq).pipe(
    catchError(err => {
      if (err.status === 401) {
        console.error('[Auth] Token inválido o expirado.');
        // Aquí podrías redirigir al login si lo necesitas
        // router.navigate(['/login']);
      }
      if (err.status === 403) {
        alert("No tienes permisos para realizar esta acción.");
      }
      if (err.status === 500) {
        //loguear errores del servidor para facilitar depuración
        console.error('[Server Error 500]', err.url, err.error);
      }
      return throwError(() => err);
    })
  );
};
