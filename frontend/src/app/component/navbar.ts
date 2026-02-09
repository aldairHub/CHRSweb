import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.scss']
})
export class NavbarComponent {

  // Propiedades para notificaciones
  showNotifications = false;
  unreadCount = 2;
  notificaciones = [
    {
      id: 1,
      titulo: 'Nueva postulación',
      mensaje: 'Tienes una nueva postulación pendiente',
      leida: false,
      tipo: 'warning',  // ← AGREGAR
      hora: 'Hace 2 horas'  // ← AGREGAR
    },
    {
      id: 2,
      titulo: 'Documento aprobado',
      mensaje: 'Tu documento ha sido aprobado',
      leida: false,
      tipo: 'success',  // ← AGREGAR
      hora: 'Hace 5 horas'  // ← AGREGAR
    },
    {
      id: 3,
      titulo: 'Entrevista programada',
      mensaje: 'Se ha programado tu entrevista',
      leida: true,
      tipo: 'info',  // ← AGREGAR
      hora: 'Hace 1 día'  // ← AGREGAR
    }
  ];

  // Propiedades del usuario
  nombreUsuario = '';
  rolUsuario = '';
  iniciales = '';

  constructor(private router: Router) {
    this.cargarDatosUsuario();
  }

  cargarDatosUsuario(): void {
    this.nombreUsuario = localStorage.getItem('usuario') || 'Usuario';
    this.rolUsuario = localStorage.getItem('rol') || 'Sin rol';
    this.iniciales = this.obtenerIniciales(this.nombreUsuario);
  }

  obtenerIniciales(nombre: string): string {
    const palabras = nombre.split(' ');
    if (palabras.length >= 2) {
      return (palabras[0][0] + palabras[1][0]).toUpperCase();
    }
    return nombre.substring(0, 2).toUpperCase();
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
  }

  logout(): void {
    // 1. Borrar datos del navegador
    localStorage.clear();

    // 2. Redirigir al login
    this.router.navigate(['/login']);
  }
}
