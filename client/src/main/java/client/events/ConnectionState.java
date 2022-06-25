package client.events;

import org.springframework.context.ApplicationEvent;

public class ConnectionState extends ApplicationEvent {

    private final boolean state;

    public ConnectionState(Object source, boolean state) {
        super(source);
        this.state = state;
    }

    public boolean isState() {
        return state;
    }
}
