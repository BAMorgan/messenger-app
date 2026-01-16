package com.example.messenger.crypto;

//interface allows us to slot in different cryptographic algos or formats
public interface MessageCrypto {
    String encrypt(Long conversationId, String plaintext);
    String decrypt(Long conversationId, String ciphertext);

}
