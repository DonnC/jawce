import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-msg-send',
  templateUrl: './msg-send.component.html',
  styleUrls: ['./msg-send.component.scss']
})
export class MsgSendComponent {

  @Input() message?: any;

}
