package com.anair.demo.component.movieratingsplitter;

import com.anair.demo.component.movieratingsplitter.config.SplitterConfig;
import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static com.google.common.base.Predicates.or;

@SpringBootApplication
@ComponentScan(basePackages = {"com.anair.demo.component.movieratingsplitter.*"})
@EnableSwagger2
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        LOGGER.info("Starting Splitter Service");
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        SplitterConfig splitterConfig = context.getBean(SplitterConfig.class);
        LOGGER.info("Application Started consuming from [{}] and producing to [{}]",
                splitterConfig.getConsumerTopic(),
                splitterConfig.getProducerTopic());
    }

    @Bean
    public Docket splitterServiceApi() {
        return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo()).select().paths(splitterServicePaths()).build();
    }

    private Predicate<String> splitterServicePaths() {
        return or(PathSelectors.regex("/producer.*"), PathSelectors.regex("/monitoring/health.*"));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("Movie Rating Splitter Service Message Producer")
                .description("REST methods for service monitoring and producing a Movie JSON message as a payload to the movie-message Kafka topic. This is a sample project for educational purposes only and free to be used and distributed")
                .termsOfServiceUrl("https://opensource.org/ToS")
                .contact(new Contact("Abhilash Nair", "http://www.github.com/abnair2016", "abnair2016@gmail.com"))
                .license("Apache License Version 2.0")
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
                .version("1.0")
                .build();
    }

}
