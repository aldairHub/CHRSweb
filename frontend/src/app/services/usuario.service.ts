import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  // Ajusta el puerto si tu backend no corre en el 8080
  private apiUrl = 'http://localhost:8080/api/usuarios';

  constructor(private http: HttpClient) {}

  // Este método debe coincidir con tu @PostMapping en UsuarioController
  crear(usuario: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, usuario);
  }

  listarTodos(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }
  // ─── Nuevos ────────────────────────────────────────────────

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token') ?? '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  // Caso 1 — Primer login
  cambiarClavePrimerLogin(claveNueva: string, claveNuevaConfirmacion: string): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/primer-login/cambiar-clave`,
      { claveNueva, claveNuevaConfirmacion },
      { headers: this.getAuthHeaders(), responseType: 'text' }
    );
  }

  // Caso 2 — Cambio voluntario
  cambiarClave(claveActual: string, claveNueva: string, claveNuevaConfirmacion: string): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/cambiar-clave`,
      { claveActual, claveNueva, claveNuevaConfirmacion },
      { headers: this.getAuthHeaders(), responseType: 'text' }
    );
  }

  // Caso 3 — Recuperar contraseña
  recuperarClave(correo: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/recuperar-clave`,
      null,
      {
        params: { correo },
        responseType: 'text'
      }
    );
  }
}
