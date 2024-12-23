import {Injectable} from "@angular/core";
import {RenderMessage} from "./RenderMessage";

@Injectable({
  providedIn: 'root'
})
export class ResponseUtil {

  constructor(
    private renderMessage: RenderMessage
  ) {
  }

  processResponse(response: any) {

  }

}
