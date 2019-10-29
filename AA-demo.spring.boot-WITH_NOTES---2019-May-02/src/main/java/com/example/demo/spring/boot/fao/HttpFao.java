package com.example.demo.spring.boot.fao;

/**
 * An implementation of Fao that stores data on a remote filesystem exposed via web-services
 * 
 * @author KunjPrasad
 *
 */
public class HttpFao implements Fao {

    private String fileStoreUrl;

    public HttpFao(String fileStoreUrl) {
        this.fileStoreUrl = fileStoreUrl;
    }
}
