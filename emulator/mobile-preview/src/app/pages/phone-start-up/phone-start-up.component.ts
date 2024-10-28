import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'app-phone-start-up',
  templateUrl: './phone-start-up.component.html',
  styleUrls: ['./phone-start-up.component.scss']
})
export class PhoneStartUpComponent implements OnInit{

  ngOnInit(): void {
    setTimeout(()=> {this.startPhone()}, 2000);
  }

  starter: boolean = false;

  startPhone() {
    this.starter = !this.starter;
    console.log("Phone started");
    setTimeout(()=> {this.starter = !this.starter}, 3000);
  }

}
