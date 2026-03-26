import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InstitucionConfig {
  idInstitucion: number;
  nombreInstitucion: string;
  direccion: string;
  correo: string;
  telefono: string;
  logoUrl: string | null;
  escudoUrl: string | null;
  appName: string;
  emailSmtp: string;
  emailHost: string;
  emailPort: number;
  emailSsl: boolean;
  tienePasswordConfigurado: boolean;
  imagenFondoUrl: string | null;
  nombreCorto?: string;
}

@Injectable({ providedIn: 'root' })
export class InstitucionAdminService {
  private readonly api = '/api/instituciones';

  constructor(private http: HttpClient) {}

  obtenerActiva(): Observable<InstitucionConfig> {
    return this.http.get<InstitucionConfig>(`${this.api}/activa`);
  }

  actualizar(id: number, data: any): Observable<InstitucionConfig> {
    return this.http.put<InstitucionConfig>(`${this.api}/${id}`, data);
  }

  uploadLogo(id: number, file: File): Observable<{ logoUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ logoUrl: string }>(`${this.api}/${id}/logo`, formData);
  }
  uploadEscudo(idInstitucion: number, file: File): Observable<{ escudoUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ escudoUrl: string }>(
      `${this.api}/${idInstitucion}/escudo`, formData
    );
  }
}
