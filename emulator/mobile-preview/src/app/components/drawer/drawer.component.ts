import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'app-drawer',
  templateUrl: './drawer.component.html',
  styleUrls: ['./drawer.component.scss']
})
export class DrawerComponent {

  @Input() isSwipableContentVisible?: boolean;
  @Input() drawerContent?: any;
  @Output() toggleDrawerEvent= new EventEmitter<any>();
  @Output() selectedOption = new EventEmitter<any>();

  selectOption(option: any) {
    console.log("Here is the option")
    this.selectedOption.emit(option)
  }

  toggleDrawer() {
    this.toggleDrawerEvent?.emit();
  }

}
