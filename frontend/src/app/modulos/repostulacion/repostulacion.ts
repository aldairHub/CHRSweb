import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ConvocatoriaService } from '../../services/convocatoria.service';

@Component({
  selector: 'app-repostulacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './repostulacion.html',
  styleUrls: ['./repostulacion.scss']
})
export class RepostulacionComponent implements OnInit {

  cedula = '';
  cedulaVerificada = false;
  verificando = false;
  enviando    = false;
  error       = '';
  exito       = false;
  mensajeExito = '';

  convocatoriaId:      number | null = null;
  nombreConvocatoria = '';

  archivoCedula:           File | null = null;
  archivoFoto:             File | null = null;
  archivoPrerrequisitos:   File | null = null;
  nombreArchivoCedula        = '';
  nombreArchivoFoto          = '';
  nombreArchivoPrerrequisitos = '';

  private apiUrl = 'http://localhost:8080/api/prepostulacion';

  constructor(
    private router:             Router,
    private route:              ActivatedRoute,
    private http:               HttpClient,
    private convocatoriaService: ConvocatoriaService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['convocatoriaId']) {
        this.convocatoriaId = +params['convocatoriaId'];
        this.convocatoriaService.obtener(this.convocatoriaId).subscribe({
          next:  (conv) => { this.nombreConvocatoria = conv.titulo; },
          error: ()     => {}
        });
      }
    });
  }

  verificarCedula(): void {
    if (!this.cedula) return;
    this.verificando = true;
    this.error = '';

    this.http.get<{ estado: string }>(`${this.apiUrl}/verificar-estado/${this.cedula}`)
      .subscribe({
        next: (res) => {
          this.verificando = false;
          if (res.estado === 'NO_EXISTE') {
            this.error = 'No se encontró ninguna solicitud con esta cédula. ' +
              'Si es su primera vez, use el formulario de registro.';
          } else if (res.estado.toLowerCase() === 'rechazada') {
            this.cedulaVerificada = true;
          } else {
            this.error = `Su solicitud tiene estado "${res.estado}". ` +
              'Solo puede re-postular si fue rechazada.';
          }
        },
        error: () => {
          this.verificando = false;
          this.error = 'Error al verificar. Intente más tarde.';
        }
      });
  }

  onFileCedulaSelected(e: any): void {
    const f = e.target.files[0];
    if (f?.type === 'application/pdf') { this.archivoCedula = f; this.nombreArchivoCedula = f.name; }
    else alert('Debe ser un archivo PDF');
  }

  onFileFotoSelected(e: any): void {
    const f = e.target.files[0];
    if (f?.type.startsWith('image/')) { this.archivoFoto = f; this.nombreArchivoFoto = f.name; }
    else alert('Debe ser una imagen (JPG, PNG)');
  }

  onFilePreSelected(e: any): void {
    const f = e.target.files[0];
    if (f?.type === 'application/pdf') { this.archivoPrerrequisitos = f; this.nombreArchivoPrerrequisitos = f.name; }
    else alert('Debe ser un archivo PDF');
  }

  enviarRepostulacion(): void {
    if (!this.archivoCedula || !this.archivoFoto || !this.archivoPrerrequisitos) {
      this.error = 'Debes subir todos los documentos requeridos.';
      return;
    }
    this.enviando = true;
    this.error    = '';

    const fd = new FormData();
    fd.append('cedula',               this.cedula);
    fd.append('archivoCedula',        this.archivoCedula,        this.nombreArchivoCedula);
    fd.append('archivoFoto',          this.archivoFoto,          this.nombreArchivoFoto);
    fd.append('archivoPrerrequisitos',this.archivoPrerrequisitos, this.nombreArchivoPrerrequisitos);
    if (this.convocatoriaId) fd.append('idConvocatoria', String(this.convocatoriaId));

    this.http.post<any>(`${this.apiUrl}/repostular`, fd).subscribe({
      next:  (res) => { this.enviando = false; this.exito = true;
        this.mensajeExito = res.mensaje || 'Re-postulación enviada exitosamente.'; },
      error: (err) => { this.enviando = false;
        this.error = err.error?.mensaje || 'Error al enviar. Intente más tarde.'; }
    });
  }

  irAInicio(): void { this.router.navigate(['/']); }
}
