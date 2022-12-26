package ca.wise.file;

import java.util.Arrays;
import java.util.List;

public class ShpSpatialFile implements ISpatialFile {

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("shp", "shx", "dbf", "sbn", "sbx", "fbn", "fbx",
                "ain", "aih", "atx", "ixs", "mxs", "prj", "xml", "cpg");
    }

    @Override
    public String getPrimaryExtension() {
        return "shp";
    }
}
