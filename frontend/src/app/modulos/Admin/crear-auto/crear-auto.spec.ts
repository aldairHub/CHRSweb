import { ComponentFixture, TestBed } from '@angular/core/testing';
// 1. Importamos el módulo de testing para el Router (porque tu constructor lo usa)
import { RouterTestingModule } from '@angular/router/testing';

// 2. Importamos el nombre EXACTO de tu clase
import { CrearEvaluadorComponent } from './crear-auto';

describe('CrearEvaluadorComponent', () => {
  let component: CrearEvaluadorComponent;
  let fixture: ComponentFixture<CrearEvaluadorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      // 3. Como tu componente es 'standalone: true', lo ponemos en imports
      // Agregamos RouterTestingModule para simular la navegación
      imports: [
        CrearEvaluadorComponent,
        RouterTestingModule
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(CrearEvaluadorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
