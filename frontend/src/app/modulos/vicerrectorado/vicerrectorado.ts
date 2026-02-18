// src/app/modulos/vicerrectorado/vicerrectorado.service.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../component/navbar';
import { FooterComponent } from '../../component/footer.component';
import {
  VicerrectoradoService,
  EstadisticasPrepostulacion
} from '../../services/vicerrectorado.service';

@Component({
  selector: 'app-vicerrectorado',
  standalone: true,
  imports: [CommonModule, NavbarComponent, FooterComponent],
  templateUrl: './vicerrectorado.html',
  styleUrls: ['./vicerrectorado.scss']
})
export class VicerrectoradoComponent implements OnInit {

  estadisticas: EstadisticasPrepostulacion = {
    total: 0,
    pendientes: 0,
    aprobadas: 0,
    rechazadas: 0
  };

  cargando = true;

  constructor(
    private router: Router,
    private svc: VicerrectoradoService
  ) {}

  ngOnInit(): void {
    this.svc.obtenerEstadisticas().subscribe({
      next: data => {
        this.estadisticas = data;
        this.cargando = false;
      },
      error: () => {
        this.cargando = false;
      }
    });
  }

  navegarA(ruta: string): void {
    this.router.navigateByUrl('/revisor/' + ruta);
  }
}
