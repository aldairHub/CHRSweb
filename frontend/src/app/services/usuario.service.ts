import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface PerfilUsuarioDTO {
  usuarioApp: string;
  correo:     string;
  fotoPerfil: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {

  private apiUrl = 'http://localhost:8080/api/usuarios';

  // ── BehaviorSubject para sincronizar foto en tiempo real ──────────────────
  // Inicializa desde localStorage si ya había una foto guardada
  private _fotoPerfil$ = new BehaviorSubject<string | null>(
    localStorage.getItem('foto_perfil') || null
  );
  /** Observable de la URL de foto de perfil; null si no tiene foto. */
  readonly fotoPerfil$ = this._fotoPerfil$.asObservable();

  constructor(private http: HttpClient) {}

  // ── Auth helpers ──────────────────────────────────────────────────────────

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token') ?? '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  // ── Métodos existentes ────────────────────────────────────────────────────

  crear(usuario: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, usuario);
  }

  listarTodos(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  cambiarClavePrimerLogin(claveNueva: string, claveNuevaConfirmacion: string): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/primer-login/cambiar-clave`,
      { claveNueva, claveNuevaConfirmacion },
      { headers: this.getAuthHeaders(), responseType: 'text' }
    );
  }

  cambiarClave(claveActual: string, claveNueva: string, claveNuevaConfirmacion: string): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/cambiar-clave`,
      { claveActual, claveNueva, claveNuevaConfirmacion },
      { headers: this.getAuthHeaders(), responseType: 'text' }
    );
  }

  recuperarClave(correo: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/recuperar-clave`,
      null,
      { params: { correo }, responseType: 'text' }
    );
  }

  // ── perfil y foto ─────────────────────────────────────────────────

  /**
   * GET /api/usuarios/mi-perfil
   * Retorna { usuarioApp, correo, fotoPerfil } del usuario autenticado.
   * Además sincroniza el BehaviorSubject y el localStorage.
   */
  obtenerMiPerfil(): Observable<PerfilUsuarioDTO> {
    return this.http.get<PerfilUsuarioDTO>(
      `${this.apiUrl}/mi-perfil`,
      { headers: this.getAuthHeaders() }
    ).pipe(
      tap(perfil => this._actualizarFoto(perfil.fotoPerfil))
    );
  }

  /**
   * PUT /api/usuarios/foto-perfil
   * Sube el archivo de imagen y retorna { fotoPerfil: url }.
   * Sincroniza automáticamente el BehaviorSubject y el localStorage.
   */
  subirFotoPerfil(file: File): Observable<{ fotoPerfil: string }> {
    const formData = new FormData();
    formData.append('foto', file);
    return this.http.put<{ fotoPerfil: string }>(
      `${this.apiUrl}/foto-perfil`,
      formData,
      { headers: new HttpHeaders({ Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` }) }
    ).pipe(
      tap(res => this._actualizarFoto(res.fotoPerfil))
    );
  }

  /** Actualiza BehaviorSubject + localStorage con la URL de foto. */
  private _actualizarFoto(url: string | null): void {
    if (url) {
      localStorage.setItem('foto_perfil', url);
    } else {
      localStorage.removeItem('foto_perfil');
    }
    this._fotoPerfil$.next(url);
  }

  /** Fuerza la actualización de la foto desde fuera del servicio (útil para logout). */
  limpiarFoto(): void {
    this._actualizarFoto(null);
  }
}
