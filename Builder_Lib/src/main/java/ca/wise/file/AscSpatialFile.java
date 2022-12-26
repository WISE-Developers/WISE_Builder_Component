package ca.wise.file;

import java.util.Arrays;
import java.util.List;

public class AscSpatialFile implements ISpatialFile {

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("asc", "prj");
    }

    @Override
    public String getPrimaryExtension() {
        return "asc";
    }
}
