package com.ecommerce.minishopee.upload.enums;

public enum ImageUploadPurpose {
    PRODUCT("mini-shopee/products"),
    SHOP_LOGO("mini-shopee/shops"),
    USER_AVATAR("mini-shopee/avatars");

    private final String folder;

    ImageUploadPurpose(String folder) {
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }
}
