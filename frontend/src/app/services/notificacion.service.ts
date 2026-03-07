import { Injectable, signal, computed, OnDestroy } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { interval, Subscription } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export interface Notificacion {
  idNotificacion: number;
  tipo: 'success' | 'error' | 'warning' | 'info';
  titulo: string;
  mensaje: string;
  leida: boolean;
  entidadTipo?: string;
  entidadId?: number;
  fechaCreacion: string;
  tiempoRelativo: string;
}

export interface NotificacionesResumen {
  noLeidas: number;
  notificaciones: Notificacion[];
}

@Injectable({ providedIn: 'root' })
export class NotificacionService implements OnDestroy {

  private readonly BASE = '/api/notificaciones';
  private readonly INTERVALO_MS = 30_000; // polling cada 30 segundos

  // ── Signals públicos ────────────────────────────────────────
  notificaciones = signal<Notificacion[]>([]);
  noLeidas       = signal<number>(0);

  // Computed: solo las no leídas (para el dropdown si se quiere filtrar)
  soloNoLeidas = computed(() =>
    this.notificaciones().filter(n => !n.leida)
  );

  private pollingSubscription?: Subscription;

  constructor(private http: HttpClient) {}

  // ── Iniciar polling (llamar al hacer login) ─────────────────
  iniciarPolling(): void {
    this.cargar(); // carga inmediata

    this.pollingSubscription = interval(this.INTERVALO_MS)
      .pipe(
        switchMap(() => this.http.get<NotificacionesResumen>(this.BASE, {
          headers: this.getHeaders()
        }).pipe(
          catchError(() => of(null))
        ))
      )
      .subscribe(data => {
        if (data) this.aplicarResumen(data);
      });
  }

  // ── Detener polling (llamar al hacer logout) ────────────────
  detenerPolling(): void {
    this.pollingSubscription?.unsubscribe();
    this.pollingSubscription = undefined;
    this.notificaciones.set([]);
    this.noLeidas.set(0);
  }

  // ── Carga manual ────────────────────────────────────────────
  cargar(): void {
    this.http.get<NotificacionesResumen>(this.BASE, {
      headers: this.getHeaders()
    }).pipe(
      catchError(() => of(null))
    ).subscribe(data => {
      if (data) this.aplicarResumen(data);
    });
  }

  // ── Marcar una como leída ───────────────────────────────────
  marcarLeida(idNotificacion: number): void {
    this.http.patch(
      `${this.BASE}/${idNotificacion}/leer`, {},
      { headers: this.getHeaders() }
    ).pipe(catchError(() => of(null))).subscribe(() => {
      // Actualizar signal localmente sin esperar polling
      this.notificaciones.update(list =>
        list.map(n => n.idNotificacion === idNotificacion
          ? { ...n, leida: true }
          : n
        )
      );
      this.recalcularNoLeidas();
    });
  }

  // ── Marcar todas como leídas ────────────────────────────────
  marcarTodasLeidas(): void {
    this.http.patch(
      `${this.BASE}/leer-todas`, {},
      { headers: this.getHeaders() }
    ).pipe(catchError(() => of(null))).subscribe(() => {
      this.notificaciones.update(list =>
        list.map(n => ({ ...n, leida: true }))
      );
      this.noLeidas.set(0);
    });
  }

  // ── Helpers privados ────────────────────────────────────────
  private aplicarResumen(data: NotificacionesResumen): void {
    this.notificaciones.set(data.notificaciones ?? []);
    this.noLeidas.set(data.noLeidas ?? 0);
  }

  private recalcularNoLeidas(): void {
    const count = this.notificaciones().filter(n => !n.leida).length;
    this.noLeidas.set(count);
  }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token') ?? '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  ngOnDestroy(): void {
    this.detenerPolling();
  }
}
