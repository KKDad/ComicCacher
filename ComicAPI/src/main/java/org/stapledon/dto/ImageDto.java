package org.stapledon.dto;

@SuppressWarnings({"squid:ClassVariableVisibilityCheck"})
public class ImageDto
{
    public ImageDto()
    {
        // No-argument constructor required for gson
    }
    public String  mimeType;
    public String  imageData;
    public Integer height;
    public Integer width;
    public String  imageRef;
}
