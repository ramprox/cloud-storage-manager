package interop.model.responses;

import java.io.Serializable;

/**
 * Класс для отправки клиенту различных возникающих ошибок при обработке запросов
 */
public class ErrorResponse implements Serializable {
    private final String errorMessage;

    public ErrorResponse(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
