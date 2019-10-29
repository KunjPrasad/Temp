package com.example.demo.spring.boot.fao;

import java.io.File;
import java.net.URI;

/**
 * An implementation of Fao that stores data on local filesystem
 * 
 * @author KunjPrasad
 *
 */
public class FileSystemFao implements Fao {

    private File rootDirectory;

    public FileSystemFao(String rootDirUri) {
        this.rootDirectory = new File(URI.create(rootDirUri));
        rootDirectory.mkdirs();
        // also running validations
        if (!rootDirectory.exists()
                || !rootDirectory.isDirectory()
                || !rootDirectory.canRead()
                || !rootDirectory.canWrite()) {
            throw new RuntimeException("Unable to create FileSystem storage using root-directory=" + rootDirUri);
        }
    }
}
