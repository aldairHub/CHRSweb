import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import {
  DocumentoService,
  PostulanteInfo,
  ProgresoPostulante,
  FaseProgresoUI
} from '../../../services/documento.service';

interface SeccionResultado {
  nombre: string;
  puntaje: number;
  maximo: number;
  detalle: { criterio: string; puntaje: number; maximo: number }[];
}

@Component({
  selector: 'app-resultados',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './resultados.html',
  styleUrls: ['./resultados.scss']
})
export class ResultadosComponent implements OnInit, OnDestroy {

  tabActiva: 'resumen' | 'meritos' | 'entrevista' | 'observaciones' | 'progreso' = 'progreso';
  cargando    = true;
  sinDatos    = false;
  ultimaActualizacion: string | null = null;

  estado: 'en_proceso' | 'aprobado' | 'rechazado' = 'en_proceso';
  postulante = { nombre: '', cedula: '', proceso: '', fecha: '' };
  resultados = { totalMeritos: 0, totalExperiencia: 0, totalAccionAfirmativa: 0, total: 0, posicion: 0, totalCandidatos: 0 };

  seccionesMeritos:     SeccionResultado[] = [];
  seccionesExperiencia: SeccionResultado[] = [];
  observaciones = '';

  // Progreso en tiempo real
  progreso:        ProgresoPostulante | null = null;
  fasesProgreso:   FaseProgresoUI[]          = [];
  cargandoProgreso = true;
  errorProgreso:   string | null             = null;

  // Selector de convocatoria
  misPostulaciones:       PostulanteInfo[] = [];
  idPostulacionSeleccion: number | null    = null;

  // Siempre visible, bloqueado si solo hay 1
  get mostrarSelector(): boolean { return this.misPostulaciones.length >= 1; }
  get selectorBloqueado(): boolean { return this.misPostulaciones.length <= 1; }

  private pollingSubscription: Subscription | null = null;
  private readonly POLLING_MS = 15_000;

  constructor(private router: Router, private documentoSvc: DocumentoService) {}

  ngOnInit(): void {
    const idUsuario = Number(localStorage.getItem('idUsuario'));
    if (!idUsuario) { this.router.navigate(['/login']); return; }

    this.documentoSvc.listarMisPostulaciones(idUsuario).subscribe({
      next: lista => {
        this.misPostulaciones      = lista;
        this.idPostulacionSeleccion = lista.length > 0 ? lista[0].idPostulacion : null;
        this.iniciarCarga(idUsuario);
      },
      error: () => this.iniciarCarga(idUsuario)
    });
  }

  ngOnDestroy(): void { this.detenerPolling(); }

  private iniciarCarga(idUsuario: number): void {
    this.documentoSvc.obtenerResultadosPostulante(idUsuario).subscribe({
      next: data => {
        if (!data || !data.puntajes) { this.sinDatos = true; this.cargando = false; return; }
        this.mapearDatos(data);
        this.cargando = false;
      },
      error: () => { this.sinDatos = true; this.cargando = false; }
    });
    this.iniciarPolling(idUsuario);
  }

  private iniciarPolling(idUsuario: number): void {
    this.detenerPolling();
    this.cargarProgreso(idUsuario);
    this.pollingSubscription = interval(this.POLLING_MS).pipe(
      switchMap(() => this.documentoSvc.obtenerMiProgreso(idUsuario, this.idPostulacionSeleccion ?? undefined))
    ).subscribe({
      next: data => this.procesarProgreso(data),
      error: () => {}
    });
  }

  private detenerPolling(): void { this.pollingSubscription?.unsubscribe(); this.pollingSubscription = null; }

  private cargarProgreso(idUsuario: number): void {
    this.documentoSvc.obtenerMiProgreso(idUsuario, this.idPostulacionSeleccion ?? undefined).subscribe({
      next: data => { this.procesarProgreso(data); this.cargandoProgreso = false; },
      error: () => { this.errorProgreso = 'No hay proceso de evaluación activo.'; this.cargandoProgreso = false; }
    });
  }

  private procesarProgreso(data: ProgresoPostulante): void {
    if (data.sinProceso) {
      this.errorProgreso = 'El proceso de evaluación aún no ha iniciado para esta convocatoria.';
      this.cargandoProgreso = false;
      return;
    }
    this.progreso      = data;
    this.fasesProgreso = data.fases ?? [];
    this.errorProgreso = null;
    this.ultimaActualizacion = new Date().toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  onConvocatoriaChange(idPostulacion: number): void {
    if (this.selectorBloqueado) return;
    this.idPostulacionSeleccion = idPostulacion;
    this.cargandoProgreso = true;
    this.progreso         = null;
    this.errorProgreso    = null;
    this.cargando         = true;
    this.sinDatos         = false;
    const idUsuario = Number(localStorage.getItem('idUsuario'));
    // Recargar puntajes detallados (méritos, entrevista, etc.)
    this.documentoSvc.obtenerResultadosPostulante(idUsuario).subscribe({
      next: data => {
        if (!data || !data.puntajes) { this.sinDatos = true; this.cargando = false; return; }
        this.mapearDatos(data);
        this.cargando = false;
      },
      error: () => { this.sinDatos = true; this.cargando = false; }
    });
    this.iniciarPolling(idUsuario);
  }

  get porcentajeProgreso(): number { return this.progreso?.progreso ?? 0; }

  get estadoGeneralLabel(): string {
    const e = this.progreso?.estadoGeneral ?? 'pendiente';
    return ({ pendiente: 'Pendiente', en_proceso: 'En proceso', completado: 'Completado', rechazado: 'No seleccionado' } as any)[e] ?? e;
  }

  get colorEstadoProgreso(): string {
    const e = this.progreso?.estadoGeneral ?? 'pendiente';
    return ({ en_proceso: 'azul', completado: 'verde', rechazado: 'rojo' } as any)[e] ?? 'gris';
  }

  iconoFase(estado: string): string {
    return ({ completada: 'pi-check-circle', en_curso: 'pi-spin pi-spinner', omitida: 'pi-minus-circle', pendiente: 'pi-circle' } as any)[estado] ?? 'pi-circle';
  }

  colorFase(estado: string): string {
    return ({ completada: 'verde', en_curso: 'azul', omitida: 'gris', pendiente: 'gris' } as any)[estado] ?? 'gris';
  }

  get decisionLabel(): string {
    const d = this.progreso?.decision;
    if (!d) return '';
    return ({ aprobado_contratar: 'Aprobado — Proceder a contratación', aprobado_espera: 'Aprobado — Lista de espera', no_aprobado: 'No aprobado', segunda_ronda: 'Segunda ronda de entrevistas' } as any)[d] ?? d;
  }

  private mapearDatos(data: any): void {
    const p = data.puntajes as Record<string, number>;
    const g = (key: string) => p[key] ?? 0;
    const est = data.estado_general ?? 'en_proceso';
    this.estado = est === 'completado' ? 'aprobado' : est === 'rechazado' ? 'rechazado' : 'en_proceso';
    this.observaciones = data.justificacion_decision ?? 'Sin observaciones registradas.';
    const merA = g('a1'), merB = g('b1')+g('b2')+g('b3')+g('b4'), merC = g('c1')+g('c2')+g('c3'), merD = g('d1')+g('d2')+g('d3'), merE = g('e1')+g('e2')+g('e3')+g('e4');
    this.resultados.totalMeritos = merA+merB+merC+merD+merE;
    const expDoc = g('exp_docencia'), expArea = g('exp_area'), entrevista = g('entrevista');
    this.resultados.totalExperiencia = expDoc+expArea+entrevista;
    const af = ['af_a','af_b','af_c','af_d','af_e','af_f','af_g','af_h','af_i'].reduce((s,k)=>s+g(k),0);
    this.resultados.totalAccionAfirmativa = af;
    this.resultados.total = this.resultados.totalMeritos+this.resultados.totalExperiencia+af;
    this.seccionesMeritos = [
      { nombre:'A) Título de cuarto nivel', puntaje:merA, maximo:20, detalle:[{criterio:'Título verificado en SENESCYT',puntaje:g('a1'),maximo:20}]},
      { nombre:'B) Experiencia docente/investigación', puntaje:merB, maximo:10, detalle:[{criterio:'Docencia universitaria',puntaje:g('b1'),maximo:10},{criterio:'Proyectos de investigación',puntaje:g('b2'),maximo:10},{criterio:'Gestión académica',puntaje:g('b3'),maximo:10},{criterio:'Otros',puntaje:g('b4'),maximo:10}]},
      { nombre:'C) Publicaciones', puntaje:merC, maximo:6, detalle:[{criterio:'Libros publicados',puntaje:g('c1'),maximo:6},{criterio:'Artículos indexados',puntaje:g('c2'),maximo:6},{criterio:'Otros',puntaje:g('c3'),maximo:6}]},
      { nombre:'D) Cursos de actualización', puntaje:merD, maximo:10, detalle:[{criterio:'Cursos aprobados',puntaje:g('d1'),maximo:10},{criterio:'Seminarios',puntaje:g('d2'),maximo:10},{criterio:'Otros',puntaje:g('d3'),maximo:10}]},
      { nombre:'E) Reconocimientos académicos', puntaje:merE, maximo:4, detalle:[{criterio:'Reconocimientos',puntaje:g('e1'),maximo:4},{criterio:'Premios',puntaje:g('e2'),maximo:4},{criterio:'Distinciones',puntaje:g('e3'),maximo:4},{criterio:'Otros',puntaje:g('e4'),maximo:4}]}
    ];
    this.seccionesExperiencia = [
      { nombre:'Experiencia en docencia', puntaje:expDoc, maximo:15, detalle:[{criterio:'Experiencia profesional en docencia',puntaje:expDoc,maximo:15}]},
      { nombre:'Experiencia en el área', puntaje:expArea, maximo:10, detalle:[{criterio:'Experiencia en área de formación',puntaje:expArea,maximo:10}]},
      { nombre:'Entrevista y clase demostrativa', puntaje:entrevista, maximo:25, detalle:[{criterio:'Puntaje de entrevista',puntaje:entrevista,maximo:25}]}
    ];
  }

  get porcentajeTotal(): number { return Math.round((this.resultados.total/104)*100); }
  get colorEstado(): string { if(this.estado==='aprobado') return 'verde'; if(this.estado==='rechazado') return 'rojo'; return 'amarillo'; }
  getBarWidth(p:number,m:number):number { if(!m) return 0; return Math.round((p/m)*100); }
  volver(): void { this.router.navigate(['/postulante']); }
}
