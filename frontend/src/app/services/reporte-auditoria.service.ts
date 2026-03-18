// src/app/services/reporte-auditoria.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ReporteAuditoriaConfig {
  // Metadatos
  titulo?:              string;
  subtitulo?:           string;
  formato:              'PDF' | 'EXCEL';
  orientacion:          'VERTICAL' | 'HORIZONTAL';

  // Filtros
  desde?:               string;
  hasta?:               string;
  usuarioApp?:          string;
  resultado?:           string;
  tabla?:               string;
  operacion?:           string;
  limite:               number;

  // Secciones
  incluirPortada:       boolean;
  incluirInstitucion:   boolean;
  incluirLogo:          boolean;
  incluirKpis:          boolean;
  incluirDetalle:       boolean;
  incluirPorTabla:      boolean;
  incluirTopUsuarios:   boolean;
  incluirExternos:      boolean;

  // Visual PDF
  colorPrimario:        string;
  mostrarNumeroPagina:  boolean;
  mostrarFechaGeneracion: boolean;

  // Excel
  excelHojasPorSeccion:      boolean;
  excelCongelarEncabezado:   boolean;
  excelFiltrosAutomaticos:   boolean;

  // Tipo
  tipoReporte: 'LOGIN' | 'CAMBIOS' | 'COMPLETO';
}

@Injectable({ providedIn: 'root' })
export class ReporteAuditoriaService {

  private readonly base = `${environment.apiUrl}/admin/auditoria/reporte`;

  constructor(private http: HttpClient) {}

  /**
   * Genera el reporte y lo descarga automáticamente en el navegador.
   * El backend devuelve el archivo como blob (PDF o Excel).
   */
  generar(cfg: ReporteAuditoriaConfig): Observable<Blob> {
    return this.http.post(
      `${this.base}/generar`,
      cfg,
      { responseType: 'blob' }
    );
  }

  /** Descarga el blob recibido del backend */
  descargar(blob: Blob, cfg: ReporteAuditoriaConfig): void {
    const ext    = cfg.formato === 'EXCEL' ? 'xlsx' : 'pdf';
    const mime   = cfg.formato === 'EXCEL'
      ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      : 'application/pdf';
    const tipo   = cfg.tipoReporte.toLowerCase();
    const fecha  = new Date().toISOString().replace(/[-:T.]/g, '').slice(0, 14);
    const nombre = `reporte_auditoria_${tipo}_${fecha}.${ext}`;

    // Re-crear el blob con el MIME type correcto
    const blobConTipo = new Blob([blob], { type: mime });
    const url  = window.URL.createObjectURL(blobConTipo);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = nombre;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }

  /** Config por defecto — se usa para inicializar el modal */
  configDefecto(filtrosActuales?: Partial<ReporteAuditoriaConfig>): ReporteAuditoriaConfig {
    return {
      titulo:                   '',
      subtitulo:                '',
      formato:                  'PDF',
      orientacion:              'HORIZONTAL',
      desde:                    filtrosActuales?.desde    ?? '',
      hasta:                    filtrosActuales?.hasta    ?? '',
      usuarioApp:               filtrosActuales?.usuarioApp ?? '',
      resultado:                filtrosActuales?.resultado  ?? '',
      tabla:                    filtrosActuales?.tabla      ?? '',
      operacion:                filtrosActuales?.operacion  ?? '',
      limite:                   500,
      incluirPortada:           true,
      incluirInstitucion:       true,
      incluirLogo:              true,
      incluirKpis:              true,
      incluirDetalle:           true,
      incluirPorTabla:          true,
      incluirTopUsuarios:       true,
      incluirExternos:          false,
      colorPrimario:            '#00A63E',
      mostrarNumeroPagina:      true,
      mostrarFechaGeneracion:   true,
      excelHojasPorSeccion:     true,
      excelCongelarEncabezado:  true,
      excelFiltrosAutomaticos:  true,
      tipoReporte:              'COMPLETO',
    };
  }
}
