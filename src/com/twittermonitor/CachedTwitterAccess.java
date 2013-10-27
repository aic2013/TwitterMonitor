package com.twittermonitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import twitter4j.Trend;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class CachedTwitterAccess extends BasicTwitterAccess {
	private Map<String, Object> cache;
	private File cacheFile;

	public CachedTwitterAccess(String cacheFilePath) {
		cacheFile = new File(cacheFilePath);
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream(cacheFile));
			cache = (Map<String, Object>) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			cache = new HashMap<String, Object>();
			try {
				cacheFile.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
				cacheFile = null;
			}
		}
	}

	@Deprecated
	@Override
	public List<Trend> getTrends() throws TwitterException {
		List<Trend> cachedTrends = (List<Trend>) cache.get("allTrends");
		if (cachedTrends == null) {
			cachedTrends = super.getTrends();
			cache.put("allTrends", cachedTrends);
			persistCache();
		}
		return cachedTrends;
	}

//	public User getUserDetailsByScreenName(String screenName) throws TwitterException{
//		Map<String, User> cachedUsers = (Map<String, User>)cache.get("usersByScreenName");
//		if(cachedUsers != null){
//				if(cachedUsers.containsKey(screenName)){
//			return cachedUsers.get(screenName);
//				}
//		}else{
//			cachedUsers = new HashMap<>();
//			cache.put("usersByScreenName", cachedUsers);
//		}
//		User usr = super.getUserDetailsByScreenName(screenName);
//		cachedUsers.put(screenName, usr);
//		
//		persistCache();
//		return usr;
//	}

	private void persistCache() {
		if (cacheFile == null)
			return;

		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(cacheFile));
			oos.writeObject(cache);
			oos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
