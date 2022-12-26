package ca.wise.file;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.io.Files;

public interface ISpatialFile {

    /**
     * Get a list of file extensions related to the file type. Will include the primary extension.
     * @return A list of file extensions. The . will not be part of the string.
     */
    List<String> getExtensions();
    
    /**
     * Get the extension of the main file in the file group.
     * @return The extension of the main file.
     */
    String getPrimaryExtension();
    
    default boolean isFileValid(Path path) {
        Objects.requireNonNull(path);
        return isFileValid(path.getFileName().toString());
    }
    
    default boolean isFileValid(String filename) {
        Objects.requireNonNull(filename);
        String ext = Files.getFileExtension(filename).toLowerCase();
        return getExtensions().stream().anyMatch(x -> x.equals(ext));
    }
    
    static ISpatialFile getSpatialFileInternal(String ext) {
        switch (ext) {
        case "shp":
            return new ShpSpatialFile();
        case "tif":
        case "tiff":
            return new TifSpatialFile(ext);
        case "asc":
            return new AscSpatialFile();
        }
        return null;
    }
    
    /**
     * Get the spatial file details for a file. The extensions will be used to find the details.
     * @param filename The name of a file to get the spatial file details for that file type.
     * @return The spatial file details for the type of file passed.
     */
    public static ISpatialFile getSpatialFile(String filename) {
        Objects.requireNonNull(filename);
        String ext = Files.getFileExtension(filename).toLowerCase();
        ISpatialFile file = getSpatialFileInternal(ext);
        if (file == null)
            return new SingleFileSpatialFile(ext);
        return file;
    }
    
    /**
     * Get the spatial file details from a collection of related files.
     * @param filenames A collection of files that are all part of the same file group.
     * @return If available the spatial file details will be returned.
     */
    public static Optional<ISpatialFile> getSpatialFile(List<String> filenames) {
        Objects.requireNonNull(filenames);
        for (String filename : filenames) {
            ISpatialFile file = getSpatialFile(filename);
            if (file != null)
                return Optional.of(file);
        }
        return Optional.empty();
    }
}
