package com.stapledon.comic;

import com.stapledon.interop.ComicItem;
import com.stapledon.interop.ComicList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@RunWith(SpringRunner.class)
@SpringBootTest(classes = ComicApiApplication.class)
public class ComicControllerTest
{
    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Before
    public void setUp() {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration(this.restDocumentation).uris()
                        .withScheme("https")
                        .withHost("comic.gilbert.ca")
                        .withPort(443))
                .apply(documentationConfiguration(this.restDocumentation)).build();

        ComicItem item = new ComicItem();
        item.id = 42;
        item.name = "Art Comics Daily";
        item.description = "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.";
        item.oldest = LocalDate.of(1995, 05, 31);
        item.newest = LocalDate.of(2007, 12, 8);

        ComicsService.comics.clear();
        ComicsService.comics.add(item);
    }


    @Test
    public void listTest() throws Exception {


        this.mockMvc.perform(get("/comics/v1/list").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                    document("{methodName}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                            responseFields(fieldWithPath("comics[]").description("List of Comics"))
                                    .and(fieldWithPath("comics[].id").description(""))
                                    .and(fieldWithPath("comics[].name").description("Name of the Comic"))
                                    .and(fieldWithPath("comics[].description").description("Description of the Comic"))
                                    .and(fieldWithPath("comics[].oldest").description("Oldest date available for retrieval."))
                                    .and(fieldWithPath("comics[].newest").description("Most recent date available for retrieval")),
                            responseHeaders(headerWithName("Content-Type").description("The Content-Type of the payload, e.g. `application/json`"))));
    }

}
