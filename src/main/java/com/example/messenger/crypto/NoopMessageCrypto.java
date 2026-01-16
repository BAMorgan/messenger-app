package com.example.messenger.crypto;

import org.springframework.stereotype.Component;

//No Operation Message
@Component //Component tag creates the class and manages it as a bean
           //Component is also available for injection
public class NoopMessageCrypto implements MessageCrypto {

    @Override
    public String encrypt(Long conversationId, String plaintext) {
        return plaintext;
    }

    @Override
    public String decrypt(Long conversationId, String ciphertext) {
        return ciphertext;
    }
}
