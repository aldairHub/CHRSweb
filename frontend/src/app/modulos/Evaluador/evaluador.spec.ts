import { ComponentFixture, TestBed } from '@angular/core/testing';
// 1. Importamos el componente CORRECTO (Evaluador)
import { EvaluadorComponent } from './evaluador';
import { RouterTestingModule } from '@angular/router/testing';


describe('EvaluadorComponent', () => {
  // 3. Definimos las variables con el tipo correcto
  let component: EvaluadorComponent;
  let fixture: ComponentFixture<EvaluadorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      // 4. Importamos el EvaluadorComponent aquí también
      imports: [EvaluadorComponent, RouterTestingModule]
    })
      .compileComponents();

    fixture = TestBed.createComponent(EvaluadorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
