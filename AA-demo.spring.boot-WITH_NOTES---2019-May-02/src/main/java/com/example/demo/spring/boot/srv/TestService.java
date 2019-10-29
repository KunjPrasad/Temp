package com.example.demo.spring.boot.srv;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.demo.spring.boot.dto.TestDTO;
import com.example.demo.spring.boot.exception.ExceptionLogLevel;
import com.example.demo.spring.boot.exception.Response400Exception;

/**
 * Service class containing business logic for TestController
 * 
 * @author KunjPrasad
 *
 */
@Service
public class TestService {

    public TestDTO getTestMessage(String message, String extraMessage) {
        testForExceptionHandler(message);
        // creating return object - services
        TestDTO testDTO = new TestDTO();
        testDTO.setMessage(message + extraMessage);
        return testDTO;
    }

    // Utility method to use message as basis for invocation of exceptions
    void testForExceptionHandler(String message) {
        if (message.length() > 100) {
            throw new NullPointerException("..just because");
        }
        if (message.length() >= 60) {
            throw new Response400Exception(null,
                    "Unable to process message",
                    "Provided [message] parameter is greater than 6",
                    ExceptionLogLevel.INFO);
        }
    }

    @Async
    public CompletableFuture<TestDTO> getAsyncTestMessage() throws InterruptedException {
        Thread.sleep(5000);
        TestDTO testDTO = new TestDTO();
        testDTO.setMessage("retunValue");
        return CompletableFuture.completedFuture(testDTO);
    }

}
