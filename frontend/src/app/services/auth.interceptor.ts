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
      if (err.status === 403) {
        // Reemplazar con el mecanismo de notificación del proyecto
        alert("No tienes permisos para realizar esta acción.");
      }
      return throwError(() => err);
    })
  );
};


// import { HttpInterceptorFn } from '@angular/common/http';
//
// export const authInterceptor: HttpInterceptorFn = (req, next) => {
//   const token = localStorage.getItem('token');
//
//   if (token) {
//     req = req.clone({
//       setHeaders: {
//         Authorization: `Bearer ${token}`
//       }
//     });
//   }
//
//   return next(req);
// };
