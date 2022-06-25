package client.network.service;

import interop.dto.AuthDto;
import interop.dto.Message;

public interface AuthenticationService {

    void signInRequest(AuthDto authDto);

    void signUpRequest(AuthDto authDto);

    void signInResponse(Message message);

    void signUpResponse(Message message);

}
