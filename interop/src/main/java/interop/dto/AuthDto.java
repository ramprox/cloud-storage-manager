package interop.dto;

import java.io.Serializable;

public class AuthDto implements Serializable {

    private final String login;

    private final String password;

    public AuthDto(String login, String password) {
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
