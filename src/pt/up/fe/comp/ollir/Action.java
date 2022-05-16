package pt.up.fe.comp.ollir;

public class Action {
    private ActionType actionType;
    private String context;

    public Action(ActionType actionType) {
        this.actionType = actionType;
        this.context = "";
    }

    public Action() {
        this.actionType = ActionType.UNDEFINED;
        this.context = "";
    }
    public Action(ActionType actionType, String context) {
        this.actionType = actionType;
        this.context = context;
    }

    public ActionType getAction() {
        return actionType;
    }

    public String getContext() {
        return context;
    }
}
