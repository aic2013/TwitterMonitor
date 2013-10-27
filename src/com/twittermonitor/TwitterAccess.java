package com.twittermonitor;

import java.util.*;

import twitter4j.Trend;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public interface TwitterAccess {
	public List<Trend> getTrends() throws TwitterException;
	public List<User> lookupUsersByScreenName(String[] screenNames) throws TwitterException;
	public void shutdown();
}
