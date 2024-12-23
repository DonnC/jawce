import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MsgSendComponent } from './msg-send.component';

describe('MsgSendComponent', () => {
  let component: MsgSendComponent;
  let fixture: ComponentFixture<MsgSendComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MsgSendComponent]
    });
    fixture = TestBed.createComponent(MsgSendComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
