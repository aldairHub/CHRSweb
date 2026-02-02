import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PostulanteComponent } from './postulante';
import { RouterTestingModule } from '@angular/router/testing';

describe('PostulanteComponent', () => {
  let component: PostulanteComponent;
  let fixture: ComponentFixture<PostulanteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PostulanteComponent, RouterTestingModule]
    })
      .compileComponents();

    fixture = TestBed.createComponent(PostulanteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
