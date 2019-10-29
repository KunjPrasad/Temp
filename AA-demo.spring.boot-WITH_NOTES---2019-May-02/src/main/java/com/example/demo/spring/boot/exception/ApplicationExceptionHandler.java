package com.example.demo.spring.boot.exception;

import javax.validation.ConstraintViolationException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ApplicationExceptionHandler {

    /**
     * Method to handle exceptions
     * 
     * @param e
     * @return
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApplicationExceptionDTO> handleApplExcp(ApplicationException e) {
        // log the error
        logException(e);
        // prepare the dto object to be returned
        ApplicationExceptionDTO dto = new ApplicationExceptionDTO();
        dto.setResponseStatus(e.getResponseStatus().value());
        dto.setReturnMessage(e.getReturnMessage());
        dto.setErrorSeverity(e.getExcpLogLevel().getResponseSeverity());
        return new ResponseEntity<ApplicationExceptionDTO>(dto, e.getResponseStatus());
    }

    // Utility method to log the exception at desired log-level
    void logException(ApplicationException e) {
        e.getExcpLogLevel().log(e.getDetailCauseMessage(), e);
    }

    /**
     * Method to handle any validation-based constraint-violation-exceptions
     * 
     * @param e
     * @return
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApplicationExceptionDTO> handleApplExcp(ConstraintViolationException e) {
        // for return message
        StringBuilder returnMsgStbl = new StringBuilder();
        e.getConstraintViolations().stream().forEach(s -> returnMsgStbl.append(s.getMessage()).append("; "));
        returnMsgStbl.delete(returnMsgStbl.length() - 2, returnMsgStbl.length());
        // for detailed message
        StringBuilder detailMsgStbl = new StringBuilder();
        e.getConstraintViolations().stream().forEach(s -> detailMsgStbl.append(String.format("error=%s, class=%s, "
                + "property=%s, value=%s", s.getMessage(), s.getRootBeanClass(), s.getPropertyPath(),
                s.getInvalidValue().toString())).append("; "));
        detailMsgStbl.delete(detailMsgStbl.length() - 2, detailMsgStbl.length());
        // prepare response
        return handleApplExcp(new Response400Exception(e,
                returnMsgStbl.toString(),
                "errorsFound=" + e.getConstraintViolations().size() + "; " + detailMsgStbl.toString(),
                ExceptionLogLevel.INFO));
    }

    /**
     * Method to handle any unknown-exceptions; By changing them to UnExpectedException and then handling it
     * 
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApplicationExceptionDTO> handleApplExcp(Exception e) {
        return handleApplExcp(new UnexpectedException(e,
                "Unable to fulfill request",
                "Encountered unexpected exception in code"));
    }

}
