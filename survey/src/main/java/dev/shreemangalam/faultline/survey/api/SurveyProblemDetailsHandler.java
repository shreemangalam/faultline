package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.domain.UnknownSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SurveyProblemDetailsHandler {
    @ExceptionHandler(UnknownSystemException.class)
    ProblemDetail unknownSystem(UnknownSystemException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Unknown system");
        return problem;
    }
}
