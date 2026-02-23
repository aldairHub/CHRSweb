import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { ConvocatoriaService, Convocatoria } from '../../services/convocatoria.service';

@Component({
  selector: 'app-convocatorias-publicas',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './convocatorias-publicas.html',
  styleUrls: ['./convocatorias-publicas.scss']
})
export class ConvocatoriasPublicasComponent implements OnInit {

  convocatorias: Convocatoria[] = [];
  cargando = true;
  mostrarModal = false;
  convocatoriaSeleccionada: Convocatoria | null = null;

  constructor(
    private convocatoriaService: ConvocatoriaService,
    private router: Router,
    private cdr: ChangeDetectorRef   // ← AGREGAR
  ) {}

  ngOnInit(): void {
    this.convocatoriaService.listarAbiertas().subscribe({
      next: (data) => {
        this.convocatorias = data;
        this.cargando = false;

        if (data.length === 0) {
          alert('No hay convocatorias activas en este momento.');
          this.router.navigate(['/']);
        }

        this.cdr.detectChanges();   // ← AGREGAR
      },
      error: (err) => {
        console.error(err);
        this.cargando = false;
        alert('Error al cargar convocatorias.');
        this.router.navigate(['/']);
        this.cdr.detectChanges();   // ← AGREGAR
      }
    });
  }
  abrirModalPostular(conv: Convocatoria): void {
    this.convocatoriaSeleccionada = conv;
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.convocatoriaSeleccionada = null;
  }

  irARegistro(): void {
    if (!this.convocatoriaSeleccionada) return;
    this.router.navigate(['/registro'], {
      queryParams: { convocatoriaId: this.convocatoriaSeleccionada.idConvocatoria }
    });
  }

  irARepostulacion(): void {
    if (!this.convocatoriaSeleccionada) return;
    this.router.navigate(['/repostulacion'], {
      queryParams: { convocatoriaId: this.convocatoriaSeleccionada.idConvocatoria }
    });
  }

  volver():   void { this.router.navigate(['/']); }
  irALogin(): void { this.router.navigate(['/login']); }
}
