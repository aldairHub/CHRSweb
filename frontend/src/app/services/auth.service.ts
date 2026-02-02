import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient, private router: Router) { }

  login(usuarioApp: string, claveApp: string): Observable<any> {
    // Enviamos el objeto al backend de Spring Boot
    return this.http.post(`${this.apiUrl}/login`, { usuarioApp, claveApp });
  }

  guardarSesion(datos: any): void {
    localStorage.setItem('usuario', datos.usuario);
    localStorage.setItem('rol', datos.rol);
    localStorage.setItem('id', datos.id);
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
