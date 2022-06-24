package client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageLocation {

    @Value("${image.directory}")
    private String imageDir;

    @Value("${image.file}")
    private String imageFile;

    @Value("${image.parentDir}")
    private String imageParentDir;

    public String getImageDir() {
        return imageDir;
    }

    public String getImageFile() {
        return imageFile;
    }

    public String getImageParentDir() {
        return imageParentDir;
    }
}
