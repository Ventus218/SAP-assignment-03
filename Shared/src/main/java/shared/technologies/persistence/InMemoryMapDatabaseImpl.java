package shared.technologies.persistence;

import java.util.*;

public class InMemoryMapDatabaseImpl implements InMemoryMapDatabase {

    private final Map<String, Map<String, Object>> maps = new HashMap<>();

    @Override
    public Map<String, Object> createMap(String mapName) {
        if (maps.containsKey(mapName)) {
            throw new IllegalStateException("A map with name " + mapName + " already exists.");
        }
        Map<String, Object> map = new HashMap<>();
        maps.put(mapName, map);
        return map;
    }

    @Override
    public void deleteMap(String mapName) {
        if (!maps.containsKey(mapName)) {
            throw new IllegalStateException("A map with name " + mapName + " does not exist.");
        }
        maps.remove(mapName);
    }

    @Override
    public Map<String, Object> getMap(String mapName) {
        if (!maps.containsKey(mapName)) {
            throw new IllegalStateException("A map with name " + mapName + " does not exist.");
        }
        return maps.get(mapName);
    }

}
