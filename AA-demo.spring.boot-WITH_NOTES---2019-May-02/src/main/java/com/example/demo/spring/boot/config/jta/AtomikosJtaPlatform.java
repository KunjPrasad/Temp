package com.example.demo.spring.boot.config.jta;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.example.demo.spring.boot.exception.CodeIntegrationException;

// See comments in JtaConfiguration.java. Essentially, with XA, Hibernate needs to be provided an implementation of AbstractJtaPlatform class
public class AtomikosJtaPlatform extends AbstractJtaPlatform {

    private static final long serialVersionUID = 1L;

    private static TransactionManager sTransactionManager;
    private static UserTransaction sUserTransaction;

    @Override
    protected TransactionManager locateTransactionManager() {
        if (sTransactionManager == null) {
            throw new CodeIntegrationException(null,
                    "Unable to process transaction",
                    "Calling transactionManager in AtomikosJtaPlatform before it is set. Error!");
        }
        return sTransactionManager;
    }

    @Override
    protected UserTransaction locateUserTransaction() {
        if (sUserTransaction == null) {
            throw new CodeIntegrationException(null,
                    "Unable to process transaction",
                    "Calling userTransaction in AtomikosJtaPlatform before it is set. Error!");
        }
        return sUserTransaction;
    }

    // Note that the 3 setter methods are synchronized - so that effective null check can be done! In reality, unless
    // someone tampers, the exception should not get raised.
    // ALSO NOTE, the properties need to be static so that they can be set just once and then that is set forever. Since
    // the properties are static, so, the setters are also static
    public synchronized static void setFromJtaTransactionManager(JtaTransactionManager jtaTransactionManager) {
        if (sTransactionManager != null && sUserTransaction != null) {
            sTransactionManager = jtaTransactionManager.getTransactionManager();
            sUserTransaction = jtaTransactionManager.getUserTransaction();
        } else {
            throw new CodeIntegrationException(null,
                    "Unable to process transaction",
                    "Setting transactionMananger or userTransaction when one/both are already set. Error!");
        }
    }

    public synchronized static void setTransactionManager(TransactionManager transactionManager) {
        if (sTransactionManager == null) {
            sTransactionManager = transactionManager;
        } else {
            throw new CodeIntegrationException(null,
                    "Unable to process transaction",
                    "Setting transactionMananger when it is already set. Error!");
        }
    }

    public synchronized static void setUserTransaction(UserTransaction userTransaction) {
        if (sUserTransaction == null) {
            sUserTransaction = userTransaction;
        } else {
            throw new CodeIntegrationException(null,
                    "Unable to process transaction",
                    "Setting userTransaction when it is already set. Error!");
        }
    }

}
