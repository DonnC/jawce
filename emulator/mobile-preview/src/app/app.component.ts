import {Component, OnInit} from '@angular/core';
import { initFlowbite } from 'flowbite';
import { Idle } from "idlejs";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit{
  title = 'whatsapp-emulator';

  ngOnInit(): void {
    initFlowbite();
    const idle = new Idle()
      .whenNotInteractive()
      .within(5, 1000)
      .do(() => this.userNotActive())
      .start();
  }

  userNotActive(){
    console.log("The user is inactive")
  }

}
