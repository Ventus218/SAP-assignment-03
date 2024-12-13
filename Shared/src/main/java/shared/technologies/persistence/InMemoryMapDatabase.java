package shared.technologies.persistence;

import java.util.*;

public interface InMemoryMapDatabase {

    /**
     * @throws IllegalStateException if a map with the given name already exists
     * @param mapName
     * @return the new map
     */
    public Map<String, Object> createMap(String mapName);

    /**
     * @throws IllegalStateException if a map with the given name does not exist
     * @param mapName
     */
    public void deleteMap(String mapName);

    /**
     * @throws IllegalStateException if a map with the given name does not exist
     * @param mapName
     * @return the searched map
     */
    public Map<String, Object> getMap(String mapName);

}
