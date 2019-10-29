package com.example.demo.spring.boot.config.jta;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;

/**
 * This configuration file helps in setting transaction-related aspects of JTA
 * 
 * @author KunjPrasad
 *
 */
@Configuration
public class JtaTxHelperConfiguration {

    @Value("${spring.jta.transaction.timeout.seconds:30}")
    private int jtaTransactionTimeout;

    @Bean
    public UserTransaction jtaUserTransaction() throws Throwable {
        UserTransactionImp userTransactionImp = new UserTransactionImp();
        userTransactionImp.setTransactionTimeout(jtaTransactionTimeout);
        // set value in AtomikosJtaPlatform - see that class for extra details
        AtomikosJtaPlatform.setUserTransaction(userTransactionImp);
        // return
        return userTransactionImp;
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public TransactionManager jtaTransactionManager() throws Throwable {
        UserTransactionManager userTransactionManager = new UserTransactionManager();
        userTransactionManager.setForceShutdown(false);
        // set value in AtomikosJtaPlatform - see that class for extra details
        AtomikosJtaPlatform.setTransactionManager(userTransactionManager);
        // return
        return userTransactionManager;
    }

    // This is the main object which is used by Spring an does the transaction management
    @Bean
    public PlatformTransactionManager jtaPlatformTransactionManager() throws Throwable {
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(jtaUserTransaction(),
                jtaTransactionManager());
        // set as needed..
        // jtaTransactionManager.setAllowCustomIsolationLevels(true);
        return jtaTransactionManager;
    }
}
