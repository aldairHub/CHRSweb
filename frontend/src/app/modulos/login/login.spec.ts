import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login'; // ✅ Importamos la clase correcta
import { FormsModule } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';

describe('LoginComponent', () => {
  // ✅ Usamos LoginComponent en lugar de Login
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        FormsModule,
        LoginComponent // ✅ Aquí también
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent); // ✅ Y aquí
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial values', () => {
    expect(component.usuarioApp).toBe('');
    expect(component.claveApp).toBe('');
    expect(component.showPassword).toBe(false);
    expect(component.isLoading).toBe(false);
    expect(component.serverError).toBe('');
  });

  it('should toggle password visibility', () => {
    expect(component.showPassword).toBe(false);

    component.togglePassword();
    expect(component.showPassword).toBe(true);

    component.togglePassword();
    expect(component.showPassword).toBe(false);
  });

  it('should show error when fields are empty', () => {
    component.usuarioApp = '';
    component.claveApp = '';

    component.onLogin();

    // ⚠️ IMPORTANTE: Este texto debe ser EXACTAMENTE IGUAL al que pusiste en login.ts
    expect(component.serverError).toBe('Por favor ingrese usuario y contraseña');
  });

  it('should not call backend when fields are empty', () => {
    component.usuarioApp = '';
    component.claveApp = '';

    const initialLoading = component.isLoading;
    component.onLogin();

    expect(component.isLoading).toBe(initialLoading);
  });

  it('should set loading state when form is valid', () => {
    component.usuarioApp = 'testuser';
    component.claveApp = 'testpass';

    // El componente intentará hacer la llamada
    component.onLogin();

    // Verificamos que se limpió el error y se activó la carga
    expect(component.serverError).toBe('');
    // Nota: Como usamos setTimeout en el componente, isLoading será true inmediatamente
    expect(component.isLoading).toBe(true);
  });
});
