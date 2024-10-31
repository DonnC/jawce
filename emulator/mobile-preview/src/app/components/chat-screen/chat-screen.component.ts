import {
  AfterViewChecked,
  AfterViewInit,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
} from '@angular/core';
import { MessageInterface } from '../../interfaces/message.interface';
import { io } from 'socket.io-client';
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-chat-screen',
  templateUrl: './chat-screen.component.html',
  styleUrls: ['./chat-screen.component.scss'],
})
export class ChatScreenComponent implements OnInit, AfterViewInit {
  @ViewChild('chatContainer') private chatContainer!: ElementRef;

  constructor(private datePipe: DatePipe, private http: HttpClient) {}

  private socket: any;

  message: string = '';

  ngAfterViewInit() {
    this.scrollToBottom();
  }

  scrollToBottom(): void {
    if (this.chatContainer && this.chatContainer.nativeElement) {
      try {
        this.chatContainer.nativeElement.scrollTop =
          this.chatContainer.nativeElement.scrollHeight;
      } catch (err) {
        console.error('Scroll to bottom failed', err);
      }
    }
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      this.sendMessage();
    }
  }

  clearUserSession() {
    this.http
      .get('http://localhost:8077/whatsapp/webhook/session/clear/263780728704')
      .subscribe({
        complete: () => console.info('user session cleared'),
        next: (v) => console.log(v),
        error: (e) =>
          console.error('Clear user session: Something went wrong', e),
      });
  }

  sendMessage() {
    let response = {
      text: {
        body: this.message,
      },
      type: 'text',
    };
    this.socket.emit('messageFromClient', response);
    this.messages.push({
      time: this.getCurrentTime(),
      recipient: true,
      type: 'text',
      body: this.message,
    });
    this.message = ''; // Clear the input field after sending
    this.scrollToBottom();
  }

  ngOnInit(): void {
    this.clearUserSession();

    this.socket = io('http://localhost:3000');

    // Listen for messages from the server
    this.socket.on('messageFromServer', (message: string) => {
      console.log('Message from server:', message);
    });

    this.socket.on('responseFromWebhook', (message: any) => {
      console.log('Response from webhook:', message);
      let response = {
        id: message.id,
        time: this.getCurrentTime(),
        recipient: false,
        type: message.type,
        body: message.content,
      };
      this.messages.push(response);
    });
  }

  buttonPressed(messageId: any) {
    this.screenContent = this.messages.find(
      (message) => message.id === messageId
    );
    console.log(this.screenContent);
    this.toggleSwipableContent();
  }

  buttonReply(buttonSelected: any) {
    console.log(buttonSelected);
    let storedMessage = {
      time: this.getCurrentTime(),
      recipient: true,
      type: 'list',
      body: buttonSelected.title,
    };

    let response = {
      type: 'interactive',
      interactive: {
        type: 'button_reply',
        button_reply: {
          id: buttonSelected.id,
          title: buttonSelected.title,
        },
      },
    };
    this.socket.emit('messageFromClient', response);
    this.messages.push(storedMessage);
  }

  screenContent?: any;

  selectListOption(option: any) {
    console.log(option);
    let storedMessage = {
      time: this.getCurrentTime(),
      recipient: true,
      type: 'list',
      body: option.title,
    };
    this.toggleSwipableContent();

    let response = {
      type: 'interactive',
      interactive: {
        type: 'list_reply',
        list_reply: {
          id: option.id,
          title: option.title,
          description: option.description,
        },
      },
    };
    this.socket.emit('messageFromClient', response);
    this.messages.push(storedMessage);
  }

  isSwipableContentVisible?: boolean = false;

  toggleSwipableContent() {
    this.isSwipableContentVisible = !this.isSwipableContentVisible;
  }

  messages: MessageInterface[] = [];

  getCurrentTime() {
    return this.datePipe.transform(new Date(), 'hh:mm') || '';
  }
}
