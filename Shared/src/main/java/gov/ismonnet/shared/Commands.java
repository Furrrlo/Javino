package gov.ismonnet.shared;

public enum Commands {
    CONNECT("YO_BRO"),
    DISCONNECT("SONO_MORTO"),
    PRESS_BUTTON("B_PRESS"),
    TURN_ON_LED("LED_ON"),
    TURN_OFF_LED("LED_OFF");

    private final String toSend;

    Commands(String toSend) {
        this.toSend = toSend;
    }

    public String getToSend() {
        return toSend;
    }

    public static Commands fromString(String msg) {
        for (Commands cmd : Commands.values())
            if(cmd.toSend.equals(msg))
                return cmd;
        return null;
    }
}
