import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DevolucionPageComponent } from './devolucion-page.component';

describe('DevolucionPageComponent', () => {
  let component: DevolucionPageComponent;
  let fixture: ComponentFixture<DevolucionPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DevolucionPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DevolucionPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
