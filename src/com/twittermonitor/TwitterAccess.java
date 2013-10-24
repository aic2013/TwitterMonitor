package com.twittermonitor;

import java.util.*;

import twitter4j.Trend;
import twitter4j.Twitter;
import twitter4j.TwitterException;

@Deprecated
public interface TwitterAccess {
	public List<Trend> getTrends() throws TwitterException;
	public void shutdown();
}
