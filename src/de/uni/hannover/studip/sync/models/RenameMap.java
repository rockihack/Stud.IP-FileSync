package de.uni.hannover.studip.sync.models;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 
 * @author Tim Kohlmeier
 *
 */

public final class RenameMap {
	
	private static final Config CONFIG = Config.getInstance();
	
	private static final RenameMap INSTANCE = new RenameMap();
	
	public HashMap<String, String> renameMap = CONFIG.readRenameMap();
	
	/**
	 * Singleton instance getter.
	 * 
	 * @return
	 */
	public static RenameMap getInstance() {
		return INSTANCE;
	}
	
	public void renamePath(Path newPath, Path oldPath) {
		
		if(renameMap == null ) {
			renameMap = new HashMap<String, String>();
		}
		
		String newPathStr = newPath.toString();
		String oldPathStr = oldPath.toString();
		
		// part 1 restore oldPath to real old path if parts were renamed (are inside map)		
		int parts = oldPathStr.split("/").length; // amount of path parts
		String[] partsChanged = new String[parts];
		int index = 0;
		
		for(HashMap.Entry<String, String> entry : renameMap.entrySet()){
		    if(oldPathStr.startsWith(entry.getValue())) {
		    	partsChanged[index] = entry.getKey();
		    	index++;
		    }
		}

		// if path was altered
		if(index != 0) {
			String[] wrongOldParts = oldPathStr.split("/");			
			Arrays.sort(partsChanged, 0, index-1);
			oldPathStr = partsChanged[index-1];
			int partPos = oldPathStr.split("/").length;
			
			for(int i = partPos; i < parts; i++) {
				oldPathStr = oldPathStr.concat("/" + wrongOldParts[i]);
			}
		}
		
		// part 2 update mapped paths values containing real oldPath/... to newPath/...

		for(HashMap.Entry<String, String> entry : renameMap.entrySet()){
		    if(entry.getKey().startsWith(oldPathStr)) {
				String[] entryParts = entry.getValue().split("/");
				String updatedEntryValue = newPathStr;
				
				for(int x = parts; x < entryParts.length; x++) {
					updatedEntryValue = updatedEntryValue.concat("/" + entryParts[x]);
				}
				
				entry.setValue(updatedEntryValue);
		    }
		}
		
		if(renameMap.containsKey(oldPathStr)) {
			// path set back to oldPath
			if(oldPathStr.equals(newPathStr)) {
				renameMap.remove(oldPathStr);
			}
		} else {
			//renameMap.put("key", "Item")
			renameMap.put(oldPathStr, newPathStr);
		}

		CONFIG.writeRenameMap(renameMap);
	}
	
	public String checkPath(String path) {
		
		if(renameMap == null ) {
			return path;
		}

		String pathCopy = path;
		path = path.substring(0, path.length()-1);
		String[] paths = path.split("/");
		
		// check if path segments are in renameMap
		for (int i = paths.length-1; i >= 0; i--) {
			System.out.println(path);
			if(renameMap.containsKey(path)) {
				path = renameMap.get(path).concat("/");
				System.out.println("rename found: " + path);
				for(int x = i + 1; x < paths.length; x++) {
					
					path = path.concat(paths[x] + "/");
				}
				System.out.println("rename fullpath: " + path);
				return path;
			}
			if(i == 0) break; // not aesthetic
			path = path.substring(0, path.length() - (paths[i].length() + 1));
		}
		
		return pathCopy;
		
	}
	
}
