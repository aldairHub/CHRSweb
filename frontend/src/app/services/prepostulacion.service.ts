import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PrepostulacionResponse {
  mensaje: string;
  correo: string;
  usuarioApp: string;
  exitoso: boolean;
  idPrepostulacion: number;
}

@Injectable({
  providedIn: 'root'
})
export class PrepostulacionService {
  private apiUrl = 'http://localhost:8080/api/prepostulacion';

  constructor(private http: HttpClient) {}

  registrarPrepostulacion(
    correo: string,
    cedula: string,
    nombres: string,
    apellidos: string,
    archivoCedula: File,
    archivoFoto: File,
    archivoPrerrequisitos: File
  ): Observable<PrepostulacionResponse> {
    const formData = new FormData();
    formData.append('correo', correo);
    formData.append('cedula', cedula);
    formData.append('nombres', nombres);
    formData.append('apellidos', apellidos);
    formData.append('archivoCedula', archivoCedula);
    formData.append('archivoFoto', archivoFoto);
    formData.append('archivoPrerrequisitos', archivoPrerrequisitos);

    return this.http.post<PrepostulacionResponse>(this.apiUrl, formData);
  }

  verificarCedula(cedula: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/verificar-cedula/${cedula}`);
  }
}
