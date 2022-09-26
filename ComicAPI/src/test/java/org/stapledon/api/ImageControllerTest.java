package org.stapledon.api;

import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(classes = ComicApiApplication.class)
public class ImageControllerTest {
//    @Rule
//    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
//
//    @Autowired
//    private WebApplicationContext context;
//
//    @MockBean
//    private IComicsService comicsService;
//
//    @MockBean
//    private IUpdateService updateService;
//
//
//    private MockMvc mockMvc;
//
//    private final List<ComicItem> comics = new ArrayList<>();
//
//    @BeforeEach
//    public void setUp()
//    {
//        MockitoAnnotations.initMocks(this);
//
//        ComicItem item1 = new ComicItem();
//        item1.id = 42;
//        item1.name = "Art Comics Daily";
//        item1.author = "Bebe Williams";
//        item1.description = "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.";
//        item1.oldest = LocalDate.of(1995, 05, 31);
//        item1.newest = LocalDate.of(2007, 12, 8);
//        item1.enabled = true;
//
//        ComicItem item2 = new ComicItem();
//        item2.id = 187;
//        item2.name = "The Dysfunctional Family Circus";
//        item2.author = "Bil Keane";
//        item2.description = "The Dysfunctional Family Circus is the name of several long-running parodies of the syndicated api strip The Family Circus, featuring either Bil Keane's artwork with altered captions, or (less often) original artwork made to appear like the targeted strips.";
//        item2.oldest = LocalDate.of(1989, 8, 31);
//        item2.newest = LocalDate.of(2013, 12, 8);
//        item2.enabled = false;
//
//        this.comics.add(item1);
//        this.comics.add(item2);
//
//        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
//                .apply(documentationConfiguration(this.restDocumentation).uris()
//                        .withScheme("https")
//                        .withHost("api.gilbert.ca")
//                        .withPort(443))
//                .apply(documentationConfiguration(this.restDocumentation)).build();
//    }
//
//    @Test
//    public void retrieveAvatarTest() throws Exception
//    {
//        Path path = Paths.get(CacheUtilsTest.getResourcesDirectory().getAbsolutePath(),"FakeComic", "avatar.png");
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
//        ResponseEntity<ImageDto> entity = new ResponseEntity<>(ImageUtils.getImageDto(path.toFile()), headers, HttpStatus.OK);
//
//        when(comicsService.retrieveAvatar(any(int.class))).thenReturn(entity);
//
//        this.mockMvc.perform(get("/api/v1/comics/{comic_id}/avatar", "42").accept(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(status().isOk())
//                .andDo(
//                        document("{methodName}",
//                                preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
//                                pathParameters(parameterWithName("comic_id").description("Specific api to retrieve")),
//                                responseFields(
//                                        fieldWithPath("mimeType").description("MimeType of the image"),
//                                        fieldWithPath("imageData").description("Base64 encoded image"),
//                                        fieldWithPath("height").description("Height of the image, in pixels"),
//                                        fieldWithPath("width").description("Width of the image, in pixels"),
//                                        fieldWithPath("imageDate").description("Date of the image returned")),
//                                responseHeaders(headerWithName("Content-Type").description("image/jpg"))));
//    }
//
//
//    @Test
//    public void retrieveFirstStripTest() throws Exception
//    {
//        Path path = Paths.get(CacheUtilsTest.getResourcesDirectory().getAbsolutePath(),"FakeComic", "avatar.png");
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
//        ResponseEntity<ImageDto> entity = new ResponseEntity<>(ImageUtils.getImageDto(path.toFile()), headers, HttpStatus.OK);
//
//        when(comicsService.retrieveComicStrip(any(int.class), any(Direction.class))).thenReturn(entity);
//
//        this.mockMvc.perform(get("/api/v1/comics/{comic_id}/strips/first", "42").accept(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(status().isOk())
//                .andDo(
//                        document("{methodName}",
//                                preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
//                                pathParameters(parameterWithName("comic_id").description("Specific api to retrieve")),
//                                responseFields(
//                                        fieldWithPath("mimeType").description("MimeType of the image"),
//                                        fieldWithPath("imageData").description("Base64 encoded image"),
//                                        fieldWithPath("height").description("Height of the image, in pixels"),
//                                        fieldWithPath("width").description("Width of the image, in pixels"),
//                                        fieldWithPath("imageDate").description("Date of the image returned")),
//                                responseHeaders(headerWithName("Content-Type").description("image/jpg"))));
//    }
//
//    @Test
//    public void retrieveLastStripTest() throws Exception
//    {
//        Path path = Paths.get(CacheUtilsTest.getResourcesDirectory().getAbsolutePath(),"FakeComic", "avatar.png");
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
//        ResponseEntity<ImageDto> entity = new ResponseEntity<>(ImageUtils.getImageDto(path.toFile()), headers, HttpStatus.OK);
//
//        when(comicsService.retrieveComicStrip(any(int.class), any(Direction.class))).thenReturn(entity);
//
//        this.mockMvc.perform(get("/api/v1/comics/{comic_id}/strips/last", "42").accept(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(status().isOk())
//                .andDo(
//                        document("{methodName}",
//                                preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
//                                pathParameters(parameterWithName("comic_id").description("Specific api to retrieve")),
//                                responseFields(
//                                        fieldWithPath("mimeType").description("MimeType of the image"),
//                                        fieldWithPath("imageData").description("Base64 encoded image"),
//                                        fieldWithPath("height").description("Height of the image, in pixels"),
//                                        fieldWithPath("width").description("Width of the image, in pixels"),
//                                        fieldWithPath("imageDate").description("Date of the image returned")),
//                                responseHeaders(headerWithName("Content-Type").description("image/jpg"))));
//    }
//
//
//    @Test
//    public void retrieveNextStripTest() throws Exception
//    {
//        Path path = Paths.get(CacheUtilsTest.getResourcesDirectory().getAbsolutePath(),"FakeComic", "avatar.png");
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
//        ResponseEntity<ImageDto> entity = new ResponseEntity<>(ImageUtils.getImageDto(path.toFile()), headers, HttpStatus.OK);
//
//        when(comicsService.retrieveComicStrip(any(int.class), any(Direction.class), any(LocalDate.class))).thenReturn(entity);
//
//        this.mockMvc.perform(get("/api/v1/comics/{comic_id}/next/{reference}", "42", "2010-06-28").accept(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(status().isOk())
//                .andDo(
//                        document("{methodName}",
//                                preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
//                                pathParameters(
//                                        parameterWithName("comic_id").description("Specific api to retrieve"),
//                                        parameterWithName("reference").description("Starting date to retrieve starting from, in yyyy-mm-dd format ")),
//                                responseFields(
//                                        fieldWithPath("mimeType").description("MimeType of the image"),
//                                        fieldWithPath("imageData").description("Base64 encoded image"),
//                                        fieldWithPath("height").description("Height of the image, in pixels"),
//                                        fieldWithPath("width").description("Width of the image, in pixels"),
//                                        fieldWithPath("imageDate").description("Date of the image returned")),
//                                responseHeaders(headerWithName("Content-Type").description("image/jpg"))));
//    }
//
//    @Test
//    public void retrievePreviousStripTest() throws Exception
//    {
//        Path path = Paths.get(CacheUtilsTest.getResourcesDirectory().getAbsolutePath(),"FakeComic", "avatar.png");
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
//        ResponseEntity<ImageDto> entity = new ResponseEntity<>(ImageUtils.getImageDto(path.toFile()), headers, HttpStatus.OK);
//
//        when(comicsService.retrieveComicStrip(any(int.class), any(Direction.class), any(LocalDate.class))).thenReturn(entity);
//
//        this.mockMvc.perform(get("/api/v1/comics/{comic_id}/previous/{reference}", "42", "2010-06-28").accept(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(status().isOk())
//                .andDo(
//                        document("{methodName}",
//                                preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
//                                pathParameters(
//                                        parameterWithName("comic_id").description("Specific api to retrieve"),
//                                        parameterWithName("reference").description("Starting date to retrieve starting from, in yyyy-mm-dd format ")),
//                                responseFields(
//                                        fieldWithPath("mimeType").description("MimeType of the image"),
//                                        fieldWithPath("imageData").description("Base64 encoded image"),
//                                        fieldWithPath("height").description("Height of the image, in pixels"),
//                                        fieldWithPath("width").description("Width of the image, in pixels"),
//                                        fieldWithPath("imageDate").description("Date of the image returned")),
//                                responseHeaders(headerWithName("Content-Type").description("image/jpg"))));
//    }
//
//
//
//    private FieldDescriptor[] comic()
//    {
//        return new FieldDescriptor[]{
//                fieldWithPath("id").description(""),
//                fieldWithPath("name").description("Name of the Comic"),
//                fieldWithPath("author").description("Author of the Comic"),
//                fieldWithPath("description").description("Description of the Comic"),
//                fieldWithPath("oldest").description("Oldest date available for retrieval."),
//                fieldWithPath("newest").description("Most recent date available for retrieval"),
//                fieldWithPath("enabled").description("Is caching of this api enabled?")
//        };
//    }
//
//
//
//
}
