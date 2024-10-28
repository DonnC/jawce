import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'app-lock-screen',
  templateUrl: './lock-screen.component.html',
  styleUrls: ['./lock-screen.component.scss']
})
export class LockScreenComponent implements OnInit{

  currentDate = Date.now();
  ngOnInit(): void {

  }

}
