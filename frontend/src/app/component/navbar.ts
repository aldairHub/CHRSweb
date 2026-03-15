import { Component, OnInit, OnDestroy, HostListener, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { filter, takeUntil } from 'rxjs/operators';
import { LogoService } from '../services/logo.service';
import { Observable, of, Subject } from 'rxjs';
import { AsyncPipe } from '@angular/common';
import { NotificacionService } from '../services/notificacion.service';
import { AuthStateService } from '../services/auth-state.service';  // ← compañero
import { ThemeService } from '../services/theme.service';            // ← tuyo
import { UsuarioService } from '../services/usuario.service';        // ← foto perfil

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

  // ── Foto de perfil (reactivo) ────────────────────────────────
  fotoPerfil: string | null = null;

  // ── Institución ─────────────────────────────────────────────
  appSubtitulo  = localStorage.getItem('inst_nombreInstitucion') || localStorage.getItem('inst_appName') || 'Sistema de selección docente';
  nombreCorto$: Observable<string> = of(localStorage.getItem('inst_nombreCorto') ?? '');

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private authService: AuthService,
    private location: Location,
    private activatedRoute: ActivatedRoute,
    public  logoService: LogoService,
    public  notifService: NotificacionService,
    private authState: AuthStateService,       // ← compañero
    private cdr: ChangeDetectorRef,            // ← tuyo
    public  themeService: ThemeService,        // ← tuyo
    private usuarioService: UsuarioService     // ← foto perfil
  ) {
    this.cargarDatosUsuario();
    this.syncIsHome();

    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.syncIsHome());
  }

  ngOnInit(): void {
    this.notifService.iniciarPolling();
    // Cargar subtítulo dinámico desde institución
    this.logoService.getNombreInstitucion().subscribe(inst => {
      if (inst) this.appSubtitulo = inst;
      else {
        const cached = localStorage.getItem('inst_appName');
        if (cached) this.appSubtitulo = cached;
      }
    });
    // Refrescar desde API y actualizar nombreCorto$ dinámicamente
    this.nombreCorto$ = this.logoService.getNombreCorto();
    this.logoService.cargar();

    // Suscribirse a la foto de perfil reactiva — se actualiza sin recargar página
    this.usuarioService.fotoPerfil$
      .pipe(takeUntil(this.destroy$))
      .subscribe(url => {
        this.fotoPerfil = url;
        this.cdr.detectChanges();
      });

    this.cdr.detectChanges();
  }

  ngOnDestroy(): void {
    this.notifService.detenerPolling();
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Cierre de dropdowns al hacer click fuera ────────────────
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.notification-wrapper')) this.showNotifications = false;
    if (!target.closest('.user-profile'))          this.showPerfilMenu   = false;
    this.cdr.detectChanges();
  }

  // ── Acciones de notificaciones ──────────────────────────────
  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    this.cdr.detectChanges();
  }

  marcarLeida(idNotificacion: number, event: MouseEvent): void {
    event.stopPropagation();
    this.notifService.marcarLeida(idNotificacion);
    this.cdr.detectChanges();
  }

  marcarTodasLeidas(event: MouseEvent): void {
    event.stopPropagation();
    this.notifService.marcarTodasLeidas();
    this.cdr.detectChanges();
  }

  irAInicio(): void {
    const rol = localStorage.getItem('rol') || '';
    this.router.navigate(['/' + rol]);
  }

  irAlHistorial(event: MouseEvent): void {
    event.stopPropagation();
    this.showNotifications = false;
    this.router.navigate(['/notificaciones']);
    this.cdr.detectChanges();
  }

  navegarANotificacion(notif: any, event: MouseEvent): void {
    event.stopPropagation();
    this.notifService.marcarLeida(notif.idNotificacion);
    this.showNotifications = false;

    const rol = localStorage.getItem('rol') || '';

    switch (notif.entidadTipo) {
      case 'PREPOSTULACION':
        if (rol === 'revisor')         this.router.navigate(['/revisor/prepostulaciones']);
        else if (rol === 'admin')      this.router.navigate(['/admin/gestion-postulante']);
        else if (rol === 'postulante') this.router.navigate(['/postulante/subir-documentos']);
        else                           this.router.navigate(['/' + rol]);
        break;
      case 'SOLICITUD':
        if (rol === 'revisor')        this.router.navigate(['/revisor/solicitudes-docente']);
        else if (rol === 'evaluador') this.router.navigate(['/evaluador/solicitar']);
        else                          this.router.navigate(['/' + rol]);
        break;
      case 'REUNION':
        if (rol === 'evaluador')       this.router.navigate(['/evaluador/entrevistas-docentes']);
        else if (rol === 'postulante') this.router.navigate(['/postulante/entrevistas']);
        else                           this.router.navigate(['/' + rol]);
        break;
      case 'PROCESO':
        if (rol === 'postulante')      this.router.navigate(['/postulante/resultados']);
        else if (rol === 'evaluador')  this.router.navigate(['/evaluador/postulantes']);
        else                           this.router.navigate(['/' + rol]);
        break;
      case 'USUARIO':
        if (rol === 'admin')           this.router.navigate(['/admin/gestion-usuarios']);
        else                           this.router.navigate(['/' + rol]);
        break;
      case 'BACKUP':
        if (rol === 'admin')           this.router.navigate(['/admin/backup']);
        else                           this.router.navigate(['/' + rol]);
        break;
      default:
        this.router.navigate(['/' + rol]);
        this.cdr.detectChanges();
    }
  }

  // ── Navegación / perfil ─────────────────────────────────────
  goBack(): void {
    this.location.back();
  }

  irAlModuloHome(): void {
    const rol = localStorage.getItem('rol');
    if (rol) {
      this.router.navigate([`/${rol}`]);
    }
  }

  logout(): void {
    this.notifService.detenerPolling();
    this.usuarioService.limpiarFoto();  // limpia foto al hacer logout
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
    this.nombreUsuario    = localStorage.getItem('usuario') || 'Usuario';
    this.rolUsuario       = this.authService.getRolNombre() || 'Sin rol';
    this.rolesDisponibles = this.authService.getRolesDisponibles();
    this.iniciales        = this.obtenerIniciales(this.nombreUsuario);
    // Cargar foto desde localStorage al iniciar (por si ya existía de sesión anterior)
    this.fotoPerfil       = localStorage.getItem('foto_perfil') || null;
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

          // Actualizar BehaviorSubject con el nuevo estado  ← compañero
          const estadoActual = this.authState.getEstado();
          this.authState.setEstado({
            ...estadoActual,
            moduloNombre: modulo.moduloNombre,
            moduloRuta:   modulo.moduloRuta,
            opciones:     modulo.opciones ?? [],
            nombreRolApp: rol.nombre
          });

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
