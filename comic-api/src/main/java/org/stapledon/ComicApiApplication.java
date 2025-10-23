package org.stapledon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OpenAPIDefinition(
        info = @Info(
                title = "Web-comics scroller",
                description = "Saturday Comics Page brought Online",
                contact = @Contact(
                        name = "ComicsApi",
                        url = "https://github.com/KKDad/ComicCacher",
                        email = "adrian@gilbert.ca"
                ),
                version = "2.0",
                license = @License(
                        name = "MIT Licence",
                        url = "https://github.com/thombergs/code-examples/blob/master/LICENSE")),
        servers = @Server(url = "http://comics.stapledon.local")
)
@SpringBootApplication
@RequiredArgsConstructor
public class ComicApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ComicApiApplication.class, args);
    }
}

