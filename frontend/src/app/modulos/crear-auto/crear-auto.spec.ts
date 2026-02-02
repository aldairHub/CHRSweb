import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CrearAuto } from './crear-auto';

describe('CrearAuto', () => {
  let component: CrearAuto;
  let fixture: ComponentFixture<CrearAuto>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CrearAuto]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CrearAuto);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
