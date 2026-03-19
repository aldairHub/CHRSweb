import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AreaConocimiento {
  idArea: number;
  nombreArea: string;
}

@Injectable({ providedIn: 'root' })
export class AreaConocimientoService {

  private apiUrl = 'http://localhost:8080/api/areas-conocimiento';

  constructor(private http: HttpClient) {}

  listar(): Observable<AreaConocimiento[]> {
    return this.http.get<AreaConocimiento[]>(this.apiUrl);
  }

  crear(nombreArea: string): Observable<AreaConocimiento> {
    return this.http.post<AreaConocimiento>(this.apiUrl, { nombreArea });
  }

  actualizar(id: number, nombreArea: string): Observable<AreaConocimiento> {
    return this.http.put<AreaConocimiento>(`${this.apiUrl}/${id}`, { nombreArea });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
