package com.stapledon.comic;

import com.stapledon.interop.ComicItem;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
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

        ComicItem item1 = new ComicItem();
        item1.id = 42;
        item1.name = "Art Comics Daily";
        item1.description = "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.";
        item1.oldest = LocalDate.of(1995, 05, 31);
        item1.newest = LocalDate.of(2007, 12, 8);

        ComicItem item2 = new ComicItem();
        item2.id = 187;
        item2.name = "The Dysfunctional Family Circus";
        item2.description = "The Dysfunctional Family Circus is the name of several long-running parodies of the syndicated comic strip The Family Circus, featuring either Bil Keane's artwork with altered captions, or (less often) original artwork made to appear like the targeted strips.";
        item2.oldest = LocalDate.of(1989, 8, 31);
        item2.newest = LocalDate.of(2013, 12, 8);

        ComicsService.comics.clear();
        ComicsService.comics.add(item1);
        ComicsService.comics.add(item2);
    }


    @Test
    public void listAllTest() throws Exception {


        this.mockMvc.perform(get("/api/v1/comics").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                    document("{methodName}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("[]").description("Array of ComicItems"))
                                    .andWithPrefix("[].", comic()),
                            responseHeaders(headerWithName("Content-Type").description("application/json"))));
    }

    @Test
    public void retieveSpecificTest() throws Exception
    {
        this.mockMvc.perform(get("/api/v1/comic/{comic_id}", "42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        document("{methodName}",
                                preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                                pathParameters(parameterWithName("comic_id").description("Specific comic to retrieve")),
                                responseFields(comic()),
                                responseHeaders(headerWithName("Content-Type").description("application/json"))));
    }

//    @Test
//    public void retieveFirstStripTest() throws Exception
//    {
//        this.mockMvc.perform(get("/api/v1/comic/{comic_id}/strips/first", "42").accept(MediaType.IMAGE_JPEG))
//                .andExpect(status().isOk())
//                .andDo(
//                        document("{methodName}",
//                                preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
//                                pathParameters(parameterWithName("comic_id").description("Specific comic to retrieve")),
//                                //responseFields(comic()),
//                                responseHeaders(headerWithName("Content-Type").description("image/jpg"))));
//    }


    private FieldDescriptor[] comic()
    {
        return new FieldDescriptor[]{
                fieldWithPath("id").description(""),
                fieldWithPath("name").description("Name of the Comic"),
                fieldWithPath("description").description("Description of the Comic"),
                fieldWithPath("oldest").description("Oldest date available for retrieval."),
                fieldWithPath("newest").description("Most recent date available for retrieval"),
                fieldWithPath("enabled").description("Is caching of this comic enabled?")
        };
    }
}
