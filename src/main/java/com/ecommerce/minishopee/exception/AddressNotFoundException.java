package com.ecommerce.minishopee.exception;

public class AddressNotFoundException extends RuntimeException{

    public AddressNotFoundException(Long id) {
        super("Không tìm thấy địa chỉ với id: " + id);
    }

}
