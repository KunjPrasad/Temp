package com.example.demo.spring.boot.config.util;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.example.demo.spring.boot.fao.Fao;
import com.example.demo.spring.boot.fao.FileSystemFao;
import com.example.demo.spring.boot.fao.HttpFao;
import com.example.demo.spring.boot.util.ProfileCondition;

// @formatter:off
/*
**VERY VERY IMPORTANT**: Adding custom objects in JNDI
* -- The discussion regarding use of JNDI as made in EntityManagerConfigUserOne.java, also applies when using it to expose objects, i.e. unless
* .. you want it global and be able to share to many applications, there isn't a point in not doing it in properties; And for security
* -- Note that jndi specs itself say that each jndi name should start with certain prefixes (See https://docs.jboss.org/author/display/AS71/JNDI+Reference)
* .. However, as discussed, unless you want to expose something globally, there is no point in having JNDI
* -- Note that various different types, and even objects (which are read from module and instantiated by JNDI) are possible to put in JNDI. See
* .. https://docs.jboss.org/author/display/WFLY8/Naming+Subsystem+Configuration
* -- Note that it says java:global is allowed.. but when making datasource it is seen that JBoss expects them to have JNDI name starting with 
* .. java:jboss/... Not sure why so, maybe because JBoss is the application that owns them! But this is constraint
* 
*
*
* Adding Antivirus on File upload: Apparantely, there seems to be no way to have antivirus-by-jar; And all places require installing something
* .. After install, process is each, they give a client and require you to call on host/port running the virus scan and they return the response
* See ClamAV and related clamav-api that can easily be deployed. 
* --|---- HOWEVER, as said, there seems to be no antivirus available that can be run from jar, so that is not done!!
* --|---- A sample (and hopefully harmless) virus can be obtained from eicar for testing.
* 
*
*
* **ALTERNATE DATA STORAGE**: Note that same java application can host multiple storage type, hiding them behind a "StorageProtocol" enumeration
* .. that then guides the delegation-code used for storage
* (1) AWS-S3: https://www.baeldung.com/aws-s3-java
* (2) Google OAuth + Google Drive: To pick up files directly from client's google drive, OR, to save a big file directly to client's google drive
* (3) Google cloud storage: https://www.baeldung.com/java-google-cloud-storage
* (4) Microsoft Onedrive storage: https://stackoverflow.com/questions/50898064/upload-file-to-onedrive-from-a-java-application
* (5) Azure Storage: See https://docs.microsoft.com/en-us/azure/storage/blobs/storage-quickstart-blobs-java       https://docs.microsoft.com/en-us/azure/storage/blobs/storage-quickstart-blobs-java-v10
* (6) Hadoop: https://stackoverflow.com/questions/38624298/to-connect-to-hadoop-using-java
*
*/
//@formatter:on

@Configuration
public class FaoConfig {

    static final String FILE_SYSTEM_STORE_PREFIX = "file:///";
    static final String HTTP_STORE_PREFIX = "http://";
    static final String HTTPS_STORE_PREFIX = "https://";

    @Value("${jndi.filestore.name:#{null}}")
    private String jndiFileStoreName;

    @Value("${filestore.uri:#{null}}")
    private String fileStoreName;

    /**
     * For embedded or test profile, use the filesystem path, and never go to JNDI
     * 
     * @return
     */
    @Bean("fao")
    @Profile({ "embedded", "test" })
    Fao fileSystemFao() {
        if (StringUtils.trimToNull(fileStoreName) == null) {
            throw new RuntimeException("Filesystem storage should be provided");
        }
        return new FileSystemFao(fileStoreName);
    }

    /**
     * For test profile, make a random folder
     * 
     * @return
     * @throws NamingException
     */
    @Bean("fao")
    @Conditional({ ProfileCondition.NotEmbedded.class, ProfileCondition.NotTest.class })
    Fao jndiFao() throws NamingException {
        if (StringUtils.trimToNull(jndiFileStoreName) == null) {
            throw new RuntimeException("JNDI file storage should be provided");
        }
        InitialContext context = new InitialContext();
        String fileStoreName = (String) context.lookup(jndiFileStoreName);
        if (fileStoreName.startsWith(FILE_SYSTEM_STORE_PREFIX)) {
            return new FileSystemFao(fileStoreName);
        } else if (fileStoreName.startsWith(HTTP_STORE_PREFIX)) {
            return new HttpFao(fileStoreName);
        } else if (fileStoreName.startsWith(HTTPS_STORE_PREFIX)) {
            return new HttpFao(fileStoreName);
        } else {
            throw new RuntimeException("Fileysystem storage provided in malformed or unrecognized protocol");
        }
    }
}
