package interop.model.requests.sign;

import java.io.Serializable;

/**
 * Абстрактный базовый класс для запросов аутентификации и регистрации
 */
public abstract class Sign implements Serializable {
    private final String login;
    private final String password;

    protected Sign(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
