import {Injectable} from "@angular/core";
import {MessageStore} from "../store/messages";
import {DatePipe} from "@angular/common";

@Injectable({
  providedIn: 'root'
})
export class RenderMessage {

  constructor(
    private messageStore: MessageStore,
    private datePipe: DatePipe
  ) {
  }

  renderMessage(msg: any) {
    let error = false;
    let time = this.currentTime();
    let recipient = true;
    let type = msg.type;
    let body;
    switch (msg.type) {
      case "text":
        body = msg.text
        break;
      case "document":
        body = msg.document
        break;
      case "interactive":
        body = msg.interactive;
        break;
      default:
        error = true;
        //should be an error
        break;
    }

    let message = {
      time: time,
      recipient: recipient,
      type: msg.type,
      body: body
    }

    this.messageStore.storeMessage(message);

  }


  currentTime() {
    const currentTime = new Date();
    return this.datePipe.transform(currentTime, 'HH:mm:ss');
  }

}
