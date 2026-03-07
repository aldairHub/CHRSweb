import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { filter } from 'rxjs/operators';
import { LogoService } from '../services/logo.service';
import { AsyncPipe } from '@angular/common';
import { NotificacionService } from '../services/notificacion.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, AsyncPipe],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.scss']
})
export class NavbarComponent implements OnInit, OnDestroy {

  // ── Notificaciones ──────────────────────────────────────────
  showNotifications = false;

  // ── Usuario ─────────────────────────────────────────────────
  nombreUsuario    = '';
  rolUsuario       = '';
  iniciales        = '';
  rolesDisponibles: any[] = [];
  isDashboard      = false;
  showPerfilMenu   = false;

  constructor(
    private router: Router,
    private authService: AuthService,
    private location: Location,
    private activatedRoute: ActivatedRoute,
    public  logoService: LogoService,
    public  notifService: NotificacionService   // ← inyectado
  ) {
    this.cargarDatosUsuario();
    this.syncIsHome();

    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.syncIsHome());
  }

  ngOnInit(): void {
    // Iniciar polling al cargar el navbar (usuario ya autenticado)
    this.notifService.iniciarPolling();
  }

  ngOnDestroy(): void {
    this.notifService.detenerPolling();
  }

  // ── Cierre de dropdowns al hacer click fuera ────────────────
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.notification-wrapper')) this.showNotifications = false;
    if (!target.closest('.user-profile'))          this.showPerfilMenu   = false;
  }

  // ── Acciones de notificaciones ──────────────────────────────
  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
  }

  marcarLeida(idNotificacion: number, event: MouseEvent): void {
    event.stopPropagation();
    this.notifService.marcarLeida(idNotificacion);
  }

  marcarTodasLeidas(event: MouseEvent): void {
    event.stopPropagation();
    this.notifService.marcarTodasLeidas();
  }

  // ── Navegación / perfil ─────────────────────────────────────
  goBack(): void {
    this.location.back();
  }

  logout(): void {
    this.notifService.detenerPolling();
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
    this.nombreUsuario   = localStorage.getItem('usuario') || 'Usuario';
    this.rolUsuario      = this.authService.getRolNombre() || 'Sin rol';
    this.rolesDisponibles = this.authService.getRolesDisponibles();
    this.iniciales       = this.obtenerIniciales(this.nombreUsuario);
  }

  obtenerIniciales(nombre: string): string {
    const palabras = nombre.split(' ');
    if (palabras.length >= 2) return (palabras[0][0] + palabras[1][0]).toUpperCase();
    return nombre.substring(0, 2).toUpperCase();
  }

  cambiarRolActivo(rol: any): void {
    this.showPerfilMenu = false;
    localStorage.setItem('rolNombre', rol.nombre);
    this.rolUsuario = rol.nombre;

    this.authService.obtenerMenuPorRol(rol.idRolApp).subscribe({
      next: (modulo: any) => {
        localStorage.setItem('modulo', JSON.stringify(modulo));
        const ruta = modulo?.moduloRuta?.replace(/^\//, '') ?? null;
        if (ruta) {
          localStorage.setItem('rol', ruta);
          this.router.navigate([`/${ruta}`], { replaceUrl: true });
        }
      },
      error: () => alert('No se pudo cambiar el rol. Intente de nuevo.')
    });
  }

  onLogoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'imgs/logo-uteq.png';
  }

  private syncIsHome(): void {
    let route = this.activatedRoute;
    while (route.firstChild) route = route.firstChild;
    this.isDashboard = route.snapshot.data?.['isHome'] === true;
  }
}
