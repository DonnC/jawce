import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {HttpClient} from "@angular/common/http";

@Component({
  selector: 'app-msg-receive',
  templateUrl: './msg-receive.component.html',
  styleUrls: ['./msg-receive.component.scss']
})
export class MsgReceiveComponent implements OnInit{

  constructor(
    private http: HttpClient
  ) {
  }

  @Input() message?: any;
  // @ts-ignore
  @Output() actionTriggered!: EventEmitter<any> = new EventEmitter();
  // @ts-ignore
  @Output() buttonReply!: EventEmitter<any> = new EventEmitter();

  pressButton() {
    this.actionTriggered!.emit(this.message.id)
  }

  replyButton(buttonSelected: any) {
    this.buttonReply!.emit(buttonSelected)
  }

  ngOnInit(): void {
    console.log(this.message);
  }

  downloadFile() {
    const fileUrl = this.message.body.link;
    console.log('File URL:', fileUrl); // Log the file URL to debug

    // Check if fileUrl starts with http:// or https:// to ensure it is an absolute URL
    if (!/^https?:\/\//i.test(fileUrl)) {
      console.error('Invalid URL:', fileUrl);
      return;
    }

    this.http.get(fileUrl, { responseType: 'blob' }).subscribe((response: Blob) => {
      const url = window.URL.createObjectURL(response);
      window.open(url); // Open the URL in a new tab
      window.URL.revokeObjectURL(url); // Revoke the URL after use
    }, (error: any) => {
      console.error('Download error:', error);
    });
  }



}
