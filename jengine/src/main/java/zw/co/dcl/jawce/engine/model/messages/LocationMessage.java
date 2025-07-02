package zw.co.dcl.jawce.engine.model.messages;


import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;

@EqualsAndHashCode(callSuper = true)
@Data
public class LocationMessage extends BaseInteractiveMessage {
    private String lat;
    private String lon;
    private String name;
    private String address;
}
