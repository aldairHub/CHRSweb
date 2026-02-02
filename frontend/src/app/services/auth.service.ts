import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = '/api/auth';

  constructor(private http: HttpClient, private router: Router) { }

  login(usuarioApp: string, claveApp: string): Observable<any> {
    // Enviamos el objeto al backend de Spring Boot
    console.log('📡 [AuthService] Enviando petición de login al backend:', usuarioApp);
    return this.http.post(`${this.apiUrl}/login`, { usuarioApp, claveApp });
  }

  guardarSesion(datos: any): void {
    // 1. Guardamos el token para futuras peticiones
    localStorage.setItem('token', datos.token);
    localStorage.setItem('usuario', datos.usuarioApp);

    // 2. Procesamos los roles que vienen del backend (LoginResponse.java)
    if (datos.roles && datos.roles.length > 0) {
      // Tomamos el primer rol y lo convertimos a minúsculas
      // Ejemplo: de "ADMIN" a "admin" para que coincida con app.routes.ts
      const rolBackend = datos.roles[0].toLowerCase();
      localStorage.setItem('rol', rolBackend);
    }
  }

  getRol(): string | null {
    return localStorage.getItem('rol');
  }

  isLoggedIn(): boolean {
    return localStorage.getItem('rol') !== null;
  }

  logout(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  redirigirPorRol(): void {
    const rol = this.getRol();
    console.log("Redirigiendo a rol:", rol); // Log para depurar

    switch(rol) {
      case 'admin': this.router.navigate(['/admin']); break;
      case 'evaluador': this.router.navigate(['/evaluador']); break;
      case 'postulante': this.router.navigate(['/postulante']); break;
      default: this.router.navigate(['/login']);
    }
  }
}
