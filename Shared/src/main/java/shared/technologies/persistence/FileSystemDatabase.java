package shared.technologies.persistence;

import java.io.File;
import java.io.IOException;

public interface FileSystemDatabase {
    public File createFile(String fileName) throws IOException;

    public void deleteFile(String fileName);

    public File getFile(String fileName);
}
