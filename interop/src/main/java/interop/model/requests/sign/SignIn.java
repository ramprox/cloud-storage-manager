package interop.model.requests.sign;

import java.io.Serializable;

/**
 * Класс для запросов аутентификации
 */
public class SignIn extends Sign implements Serializable {
    public SignIn(String login, String password) {
        super(login, password);
    }
}
