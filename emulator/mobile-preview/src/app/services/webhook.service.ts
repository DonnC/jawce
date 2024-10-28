import { Injectable } from '@angular/core';
import {API_ENDPOINTS} from "../constants/endpoints";
import {Subject} from "rxjs";
import {HttpClient} from "@angular/common/http";

@Injectable({
  providedIn: 'root'
})
export class WebhookService {

  constructor(
    private http: HttpClient
  ) { }

  webhookResponse$ = new Subject();

  sendResponse(body: any) {
    this.http.post(API_ENDPOINTS.send_response, body).subscribe((response: any) => {
      this.webhookResponse$.next(response);
    });
  }


}
