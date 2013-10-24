package com.twittermonitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import twitter4j.GeoLocation;
import twitter4j.Location;
import twitter4j.ResponseList;
import twitter4j.Trend;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.api.TrendsResources;

@Deprecated
public class BasicTwitterAccess implements TwitterAccess {
	private Twitter twitter;

	public BasicTwitterAccess(){
		twitter = TwitterFactory.getSingleton();
	}
	
	@Override
	public List<Trend> getTrends() throws TwitterException {
//		twitter.getRateLimitStatus(resources);
		TrendsResources trendsRes = twitter.trends();
		ResponseList<Location> trendLocations = trendsRes.getAvailableTrends();
		List<Trend> randomTrends = new ArrayList<Trend>();
		/* select on location at random - querying all locations exceeds the rate limit */
		Random r = new Random(new Date().getTime());
		Location randomTrendLocation = trendLocations.get(r.nextInt(trendLocations.size()));
		randomTrends.addAll(Arrays.asList(trendsRes.getPlaceTrends(randomTrendLocation.getWoeid()).getTrends()));
		return randomTrends;
	}

	@Override
	public void shutdown() {
		twitter.shutdown();
	}
}
