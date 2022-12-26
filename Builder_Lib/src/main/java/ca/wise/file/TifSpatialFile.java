package ca.wise.file;

import java.util.Arrays;
import java.util.List;

public class TifSpatialFile implements ISpatialFile {
    
    private String tifExtension;
    
    TifSpatialFile(String tifExtension) {
        this.tifExtension = tifExtension;
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList(tifExtension, "tfw", "prj");
    }

    @Override
    public String getPrimaryExtension() {
        return tifExtension;
    }
}
