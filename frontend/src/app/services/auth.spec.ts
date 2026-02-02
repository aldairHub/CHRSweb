import { TestBed } from '@angular/core/testing';
// 1. IMPORTANTE: Estas dos líneas son obligatorias para que no falle el constructor
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

// 2. Importamos tu servicio (asegúrate que el archivo se llame auth.service.ts)
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      // 3. Aquí configuramos los módulos falsos para HTTP y Router
      imports: [
        HttpClientTestingModule,
        RouterTestingModule
      ],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
