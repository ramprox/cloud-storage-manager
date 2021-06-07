package interop.model.requests.sign;

import java.io.Serializable;

/**
 * Класс для запросов регистрации
 */
public class SignUp extends Sign implements Serializable {
    public SignUp(String login, String password) {
        super(login, password);
    }
}
