package zw.co.dcl.jawce.engine.model.core;

import lombok.Data;

@Data
public class EngineRoute {
    private String userInput;
    private String nextStage;
    private boolean isRegex = false;
}
