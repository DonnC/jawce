import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChatScreenComponent } from './chat-screen.component';

describe('ChatScreenComponent', () => {
  let component: ChatScreenComponent;
  let fixture: ComponentFixture<ChatScreenComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ChatScreenComponent]
    });
    fixture = TestBed.createComponent(ChatScreenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
