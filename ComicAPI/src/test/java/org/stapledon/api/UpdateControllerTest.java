package org.stapledon.api;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.stapledon.dto.ComicItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = ComicApiApplication.class)
public class UpdateControllerTest
{
    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private IComicsService comicsService;

    @MockBean
    private IUpdateService updateService;


    private MockMvc mockMvc;

    private final List<ComicItem> comics = new ArrayList<>();

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        ComicItem item1 = new ComicItem();
        item1.id = 42;
        item1.name = "Art Comics Daily";
        item1.author = "Bebe Williams";
        item1.description = "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.";
        item1.oldest = LocalDate.of(1995, 05, 31);
        item1.newest = LocalDate.of(2007, 12, 8);
        item1.enabled = true;

        ComicItem item2 = new ComicItem();
        item2.id = 187;
        item2.name = "The Dysfunctional Family Circus";
        item2.author = "Bil Keane";
        item2.description = "The Dysfunctional Family Circus is the name of several long-running parodies of the syndicated api strip The Family Circus, featuring either Bil Keane's artwork with altered captions, or (less often) original artwork made to appear like the targeted strips.";
        item2.oldest = LocalDate.of(1989, 8, 31);
        item2.newest = LocalDate.of(2013, 12, 8);
        item2.enabled = false;

        this.comics.add(item1);
        this.comics.add(item2);

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration(this.restDocumentation).uris()
                        .withScheme("https")
                        .withHost("api.gilbert.ca")
                        .withPort(443))
                .apply(documentationConfiguration(this.restDocumentation)).build();
    }


    @Test
    public void updateAllComics() throws Exception
    {
        when(updateService.updateAll()).thenReturn(Boolean.TRUE);

        this.mockMvc.perform(get("/api/v1/update").accept(MediaType.ALL_VALUE))
                .andExpect(status().isOk())
                .andDo(
                        document("{methodName}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }

    @Test
    public void updateSpecificComic()  throws Exception
    {
        when(updateService.updateComic(42)).thenReturn(Boolean.TRUE);

        this.mockMvc.perform(get("/api/v1/update/42").accept(MediaType.ALL_VALUE))
                .andExpect(status().isOk())
                .andDo(
                        document("{methodName}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }
}
