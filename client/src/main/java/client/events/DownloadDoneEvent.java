package client.events;

import org.springframework.context.ApplicationEvent;

public class DownloadDoneEvent extends ApplicationEvent {

    private final String destination;

    public DownloadDoneEvent(Object source, String destination) {
        super(source);
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

}
