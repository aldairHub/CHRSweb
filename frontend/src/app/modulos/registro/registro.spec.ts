import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing'; // <--- IMPORTANTE
import { RegistroComponent } from './registro';

declare var jasmine: any;
declare var spyOn: any;

describe('RegistroComponent', () => {
  let component: RegistroComponent;
  let fixture: ComponentFixture<RegistroComponent>;
  let routerSpy: any;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        RegistroComponent,
        HttpClientTestingModule // <--- NECESARIO para simular peticiones HTTP
      ],
      providers: [{ provide: Router, useValue: routerSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(RegistroComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should validate all 3 files required', () => {
    spyOn(window, 'alert');

    // Simular datos de texto llenos
    component.cedula = '1234567890';
    component.nombres = 'Test User';

    // 1. Falta todo
    component.archivoCedula = null;
    component.archivoFoto = null;
    component.archivoPrerrequisitos = null;
    expect(component.validarFormulario()).toBe(false);

    // 2. Solo tiene Cédula
    component.archivoCedula = { name: 'c.pdf' } as any;
    expect(component.validarFormulario()).toBe(false);

    // 3. Tiene Cédula y Foto, pero faltan Pre-requisitos
    component.archivoFoto = { name: 'f.jpg' } as any;
    expect(component.validarFormulario()).toBe(false);
    expect(window.alert).toHaveBeenCalledWith('Debe subir la documentación de Pre-requisitos (Títulos, Experiencia)');

    // 4. Tiene todo
    component.archivoPrerrequisitos = { name: 'pre.pdf' } as any;
    expect(component.validarFormulario()).toBe(true);
  });
});
