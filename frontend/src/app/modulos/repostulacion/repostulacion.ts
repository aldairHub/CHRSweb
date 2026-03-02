import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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

  idSolicitud: number | null = null;

  archivoCedula:              File | null = null;
  archivoFoto:                File | null = null;
  archivoPrerrequisitos:      File | null = null;
  nombreArchivoCedula         = '';
  nombreArchivoFoto           = '';
  nombreArchivoPrerrequisitos = '';

  private apiUrl = 'http://localhost:8080/api/prepostulacion';

  constructor(
    private router: Router,
    private route:  ActivatedRoute,
    private http:   HttpClient,
    private cdr:    ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['idSolicitud']) {
        this.idSolicitud = +params['idSolicitud'];
      } else {
        // Si no viene idSolicitud, redirigir a convocatorias
        this.router.navigate(['/convocatorias']);
      }
    });
  }

  verificarCedula(): void {
    if (!this.cedula) return;
    this.verificando = true;
    this.error = '';

    this.http.get<any>(`${this.apiUrl}/verificar-estado/${this.cedula}`).subscribe({
      next: (res) => {
        this.verificando = false;
        if (!res.encontrado) {
          this.error = 'No se encontró ninguna solicitud con esta cédula.';
        } else if (res.estado?.toUpperCase() === 'RECHAZADO') {
          this.cedulaVerificada = true;
        } else {
          this.error = `Su solicitud tiene estado "${res.estado}". Solo puede re-postular si fue rechazada.`;
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.verificando = false;
        this.error = 'Error al verificar. Intente más tarde.';
        this.cdr.detectChanges();
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
    if (!this.idSolicitud) {
      this.error = 'No se encontró la solicitud. Vuelve a seleccionar la plaza.';
      return;
    }

    this.enviando = true;
    this.error    = '';

    const fd = new FormData();
    fd.append('cedula',                this.cedula);
    fd.append('idSolicitud',           String(this.idSolicitud));
    fd.append('archivoCedula',         this.archivoCedula,         this.nombreArchivoCedula);
    fd.append('archivoFoto',           this.archivoFoto,           this.nombreArchivoFoto);
    fd.append('archivoPrerrequisitos', this.archivoPrerrequisitos,  this.nombreArchivoPrerrequisitos);

    this.http.post<any>(`${this.apiUrl}/repostular`, fd).subscribe({
      next: (res) => {
        this.enviando    = false;
        this.exito       = true;
        this.mensajeExito = res.mensaje || 'Re-postulación enviada exitosamente.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.enviando = false;
        this.error    = err.error?.mensaje || 'Error al enviar. Intente más tarde.';
        this.cdr.detectChanges();
      }
    });
  }

  irAInicio(): void { this.router.navigate(['/']); }
}
