package shared.technologies.persistence;

import java.io.File;
import java.io.IOException;

public class FileSystemDatabaseImpl implements FileSystemDatabase {

    private final File storageDir;

    public FileSystemDatabaseImpl(File storageDir) {
        String errorMessagePrefix = "Given storage directory " + storageDir.getPath();
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            throw new IllegalArgumentException(errorMessagePrefix + " directory didn't exist and couldn't be breated");
        }
        if (!storageDir.isDirectory()) {
            throw new IllegalArgumentException(errorMessagePrefix + " is not a directory");
        }
        if (!storageDir.canWrite()) {
            throw new IllegalArgumentException(errorMessagePrefix + " cannot be written");
        }
        if (!storageDir.canRead()) {
            throw new IllegalArgumentException(errorMessagePrefix + " cannot be read");
        }

        this.storageDir = storageDir;
    }

    @Override
    public File createFile(String fileName) throws IOException {
        File file = new File(storageDir, fileName);
        if (!file.createNewFile()) {
            throw new IllegalStateException("File " + file.getPath() + " already exists");
        }
        return file;
    }

    @Override
    public void deleteFile(String fileName) {
        File file = new File(storageDir, fileName);
        if (!file.exists()) {
            throw new IllegalStateException("File " + file.getPath() + " does not exist");
        }
        if (!file.delete()) {
            throw new IllegalStateException("Something went wrong while deleting " + file.getPath());
        }
    }

    @Override
    public File getFile(String fileName) {
        File file = new File(storageDir, fileName);
        if (!file.exists()) {
            throw new IllegalStateException("File " + file.getPath() + " does not exist");
        }
        return file;
    }

}
