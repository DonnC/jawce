import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LockScreenComponent } from './lock-screen.component';

describe('LockScreenComponent', () => {
  let component: LockScreenComponent;
  let fixture: ComponentFixture<LockScreenComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LockScreenComponent]
    });
    fixture = TestBed.createComponent(LockScreenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
