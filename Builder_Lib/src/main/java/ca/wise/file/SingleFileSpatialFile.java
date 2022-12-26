package ca.wise.file;

import java.util.Collections;
import java.util.List;

public class SingleFileSpatialFile implements ISpatialFile {
    
    private final String extension;
    
    SingleFileSpatialFile(String extension) {
        this.extension = extension;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList(extension);
    }

    @Override
    public String getPrimaryExtension() {
        return extension;
    }
}
