import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class EntrevistasEstadoService {
  private idSolicitudSubject = new BehaviorSubject<number | null>(null);
  idSolicitud$ = this.idSolicitudSubject.asObservable();

  setIdSolicitud(id: number): void {
    this.idSolicitudSubject.next(id);
  }

  getIdSolicitud(): number | null {
    return this.idSolicitudSubject.getValue();
  }

  limpiar(): void {
    this.idSolicitudSubject.next(null);
  }
}
