import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { TextComponent } from './responses/text/text.component';
import { ButtonComponent } from './responses/button/button.component';
import { DocumentComponent } from './responses/document/document.component';
import { ListComponent } from './responses/list/list.component';
import { FlowComponent } from './responses/flow/flow.component';
import { DrawerComponent } from './components/drawer/drawer.component';
import { MsgSendComponent } from './components/msg-send/msg-send.component';
import { MsgReceiveComponent } from './components/msg-receive/msg-receive.component';
import { ChatScreenComponent } from './components/chat-screen/chat-screen.component';
import { HomeScreenComponent } from './pages/home-screen/home-screen.component';
import { LockScreenComponent } from './pages/lock-screen/lock-screen.component';
import { PhoneStartUpComponent } from './pages/phone-start-up/phone-start-up.component';
import {HttpClientModule} from "@angular/common/http";
import {FormsModule} from "@angular/forms";
import {DatePipe} from "@angular/common";
import { TimeFormatPipe } from './util/pipe/time-format.pipe';

@NgModule({
  declarations: [
    AppComponent,
    TextComponent,
    ButtonComponent,
    DocumentComponent,
    ListComponent,
    FlowComponent,
    DrawerComponent,
    MsgSendComponent,
    MsgReceiveComponent,
    ChatScreenComponent,
    HomeScreenComponent,
    LockScreenComponent,
    PhoneStartUpComponent,
    TimeFormatPipe,
    TimeFormatPipe
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule
  ],
  providers: [DatePipe],
  bootstrap: [AppComponent]
})
export class AppModule { }
