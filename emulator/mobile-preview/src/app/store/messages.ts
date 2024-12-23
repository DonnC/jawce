import {Injectable} from "@angular/core";
import {MessageInterface} from "../interfaces/message.interface";

@Injectable({
  providedIn: 'root'
})
export class MessageStore {

  messages: any[] = [];

  getMessages() {
    return localStorage.getItem('messages');
  }

  storeMessage(message: any) {
    let messages = this.getMessages();
    this.messages.push(message);
    this.updateMessageStore(this.messages);
  }

  updateMessageStore(messagesUpdate: any) {
    localStorage.setItem('messages', JSON.stringify(messagesUpdate));
  }

}
