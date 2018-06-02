package com.anair.demo.component.movieratingsplitter.route;

import com.anair.demo.component.movieratingsplitter.exception.SplitterException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.camel.ExchangeTimedOutException;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import static com.anair.demo.component.movieratingsplitter.util.SplitterConstants.EXCEPTION_MESSAGE;
import static com.anair.demo.component.movieratingsplitter.util.SplitterConstants.EXCEPTION_STACKTRACE;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.camel.LoggingLevel.DEBUG;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.springframework.http.HttpStatus.*;

public abstract class ExceptionHandlingRoute extends RouteBuilder {

    private static final String MOCK_SHUTDOWN_ROUTE = "mock:shutdownRoute";
    private final String className = getClass().getSimpleName();

    @Override
    public void configure() {

        onException(InvalidFormatException.class)
                .routeId("invalid-format-exception-route-id")
                .log(ERROR, className, EXCEPTION_MESSAGE)
                .log(DEBUG, className, EXCEPTION_STACKTRACE)
                .handled(true)
                .removeHeaders("*")
                .setHeader(HTTP_RESPONSE_CODE, constant(BAD_REQUEST))
                .setBody(simple("Error transforming request. Please ensure request format is correct."))
                .to(MOCK_SHUTDOWN_ROUTE)
                .end();

        onException(SplitterException.class)
                .routeId("splitter-exception-route-id")
                .log(ERROR, className, EXCEPTION_MESSAGE)
                .log(ERROR, className, EXCEPTION_STACKTRACE)
                .handled(true)
                .removeHeaders("*")
                .setHeader(HTTP_RESPONSE_CODE, constant(BAD_REQUEST))
                .setBody(constant("SplitterException: Splitter exception encountered."))
                .to(MOCK_SHUTDOWN_ROUTE)
                .end();

        onException(ExchangeTimedOutException.class)
                .routeId("exchange-timed-out-exception-route-id")
                .log(ERROR, className, EXCEPTION_MESSAGE)
                .log(ERROR, className, EXCEPTION_STACKTRACE)
                .handled(true)
                .removeHeaders("*")
                .setHeader(HTTP_RESPONSE_CODE, constant(SERVICE_UNAVAILABLE))
                .setBody(simple("Exchange Timed out\nReference: ${property.msgId}"))
                .to(MOCK_SHUTDOWN_ROUTE)
                .end();

        onException(ResourceAccessException.class)
                .routeId("resource-access-exception-route-id")
                .log(ERROR, className, EXCEPTION_MESSAGE)
                .log(ERROR, className, EXCEPTION_STACKTRACE)
                .handled(true)
                .removeHeaders("*")
                .setHeader(HTTP_RESPONSE_CODE, constant(SERVICE_UNAVAILABLE))
                .setBody(simple("Resource access exception occurred."))
                .to(MOCK_SHUTDOWN_ROUTE)
                .end();

        onException(JsonMappingException.class)
                .routeId("json-mapping-exception-route-id")
                .log(ERROR, className, "Incorrect input for JSON msg: " + EXCEPTION_MESSAGE)
                .log(ERROR, className, EXCEPTION_STACKTRACE)
                .handled(true)
                .removeHeaders("*")
                .setHeader(HTTP_RESPONSE_CODE, constant(BAD_REQUEST))
                .setProperty("exceptionMsg", simple("${exception.message}"))
                .process(exchange -> {
                    String msg = exchange.getProperty("exceptionMsg").toString();
                    String problem = msg.substring(msg.indexOf(':'), msg.indexOf('\n'));
                    exchange.getOut().setBody("The JSON received contained invalid data" + problem);
                })
                .to(MOCK_SHUTDOWN_ROUTE)
                .end();

        onException(JsonParseException.class)
                .routeId("json-parse-exception-route-id")
                .log(ERROR, className, "Invalid input for JSON msg: " + EXCEPTION_MESSAGE)
                .log(ERROR, className, EXCEPTION_STACKTRACE)
                .handled(true)
                .removeHeaders("*")
                .setHeader(HTTP_RESPONSE_CODE, constant(BAD_REQUEST))
                .setBody(simple("The JSON received was invalid"))
                .to(MOCK_SHUTDOWN_ROUTE)
                .end();

        onException(HttpServerErrorException.class)
                .routeId("http-server-error-exception-route-id")
                .log(ERROR, className, EXCEPTION_MESSAGE)
                .log(ERROR, className, EXCEPTION_STACKTRACE)
                .handled(true)
                .removeHeaders("*")
                .setHeader(HTTP_RESPONSE_CODE, constant(INTERNAL_SERVER_ERROR))
                .setBody(simple("Server Error occurred."))
                .to(MOCK_SHUTDOWN_ROUTE)
                .end();

        onException(RuntimeException.class)
                .routeId("runtime-exception-route-id")
                .log(ERROR, className, "Runtime Exception: " + EXCEPTION_MESSAGE)
                .log(ERROR, className, EXCEPTION_STACKTRACE)
                .handled(true)
                .removeHeaders("*")
                .setHeader(HTTP_RESPONSE_CODE, constant(INTERNAL_SERVER_ERROR))
                .setBody(simple("An error occurred."))
                .to(MOCK_SHUTDOWN_ROUTE)
                .end();

        onException(Throwable.class)
                .routeId("exception-route-id")
                .log(ERROR, className, "Unhandled error occurred: " + EXCEPTION_MESSAGE)
                .log(ERROR, className, EXCEPTION_STACKTRACE)
                .handled(true)
                .removeHeaders("*")
                .setHeader(HTTP_RESPONSE_CODE, constant(INTERNAL_SERVER_ERROR))
                .setBody(simple("An error occurred."))
                .to(MOCK_SHUTDOWN_ROUTE)
                .end();

    }
}
