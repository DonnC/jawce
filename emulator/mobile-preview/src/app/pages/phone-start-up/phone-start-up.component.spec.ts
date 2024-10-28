import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PhoneStartUpComponent } from './phone-start-up.component';

describe('PhoneStartUpComponent', () => {
  let component: PhoneStartUpComponent;
  let fixture: ComponentFixture<PhoneStartUpComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PhoneStartUpComponent]
    });
    fixture = TestBed.createComponent(PhoneStartUpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
