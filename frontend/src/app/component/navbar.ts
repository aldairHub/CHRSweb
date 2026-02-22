import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Location } from '@angular/common';
import { ActivatedRoute, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';


@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
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

  isDashboard = false;
  showPerfilMenu = false;
  constructor(
    private router: Router,
    private authService: AuthService,
    private location: Location,
    private activatedRoute: ActivatedRoute
  ) {
    this.cargarDatosUsuario();
    this.syncIsHome();

    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.syncIsHome());
  }

  goBack(): void {
    this.location.back();
  }

  private syncIsHome(): void {
    let route = this.activatedRoute;
    while (route.firstChild) route = route.firstChild; // llega al último hijo activo

    this.isDashboard = route.snapshot.data?.['isHome'] === true;
  }


  logout(): void {
    this.authService.logoutYSalir();
  }
  togglePerfilMenu(): void {
    this.showPerfilMenu = !this.showPerfilMenu;
  }
  irAlPerfil(): void {
    this.showPerfilMenu = false;
    this.router.navigate(['/perfil']);
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


  private cerrarSesion(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }



}
