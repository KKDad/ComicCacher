package org.stapledon.utils;

import org.junit.Assert;
import org.junit.Test;
import org.stapledon.CacheUtilsTest;
import org.stapledon.dto.ImageDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class ImageUtilsTest
{

    @Test
    public void getImageDtoAvatar()
    {
        try {
            Path path = Paths.get(CacheUtilsTest.getResourcesDirectory().getAbsolutePath(),"FakeComic", "avatar.png");
            ImageDto imageDto = ImageUtils.getImageDto(path.toFile());

            Assert.assertEquals("image/png", imageDto.mimeType);
            Assert.assertEquals(1, (int)imageDto.height);
            Assert.assertEquals(1, (int)imageDto.width);
            Assert.assertEquals("image/png", imageDto.mimeType);
            Assert.assertNull(imageDto.imageDate);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void getImageDtoStrip()
    {
        try {
            Path path = Paths.get(CacheUtilsTest.getResourcesDirectory().getAbsolutePath(),"FakeComic", "2008", "2008-01-11.png");
            ImageDto imageDto = ImageUtils.getImageDto(path.toFile());

            Assert.assertEquals("image/png", imageDto.mimeType);
            Assert.assertEquals(1, (int)imageDto.height);
            Assert.assertEquals(1, (int)imageDto.width);
            Assert.assertEquals("image/png", imageDto.mimeType);
            Assert.assertEquals(LocalDate.of(2008, 01, 11), imageDto.imageDate);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }


}