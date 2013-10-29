package com.twittermonitor;

import java.util.*;

import twitter4j.GeoLocation;
import twitter4j.Location;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Trend;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.api.TrendsResources;
import twitter4j.api.UsersResources;

public class BasicTwitterAccess implements TwitterAccess {
	private Twitter twitter;

	public BasicTwitterAccess() {
		twitter = TwitterFactory.getSingleton();
	}

	@Deprecated
	@Override
	public List<Trend> getTrends() throws TwitterException {
		// twitter.getRateLimitStatus(resources);
		TrendsResources trendsRes = twitter.trends();
		ResponseList<Location> trendLocations = trendsRes.getAvailableTrends();
		List<Trend> randomTrends = new ArrayList<Trend>();
		/*
		 * select on location at random - querying all locations exceeds the
		 * rate limit
		 */
		Random r = new Random(new Date().getTime());
		Location randomTrendLocation = trendLocations.get(r
				.nextInt(trendLocations.size()));
		randomTrends.addAll(Arrays.asList(trendsRes.getPlaceTrends(
				randomTrendLocation.getWoeid()).getTrends()));
		return randomTrends;
	}

	@Override
	public void shutdown() {
		twitter.shutdown();
	}

	@Override
	public List<User> lookupUsersByScreenName(String[] screenNames)
			throws TwitterException {
		List<User> result = new ArrayList<User>();
		Map<String, RateLimitStatus> statuses = twitter
				.getRateLimitStatus("users");
		RateLimitStatus status = statuses.get("/users/lookup");
		int remainingCalls = status.getRemaining();
		UsersResources userRes = twitter.users();
		int numIterations = screenNames.length / 100
				+ (screenNames.length % 100 == 0 ? 0 : 1);

		try {
			for (int i = 0; i < numIterations; i++) {
				while (remainingCalls <= 0) {
					int resetTime = twitter.getRateLimitStatus("users")
							.get("/users/lookup").getSecondsUntilReset();
					Thread.sleep((resetTime + 5) * 1000);
					remainingCalls = twitter.getRateLimitStatus("users")
							.get("/users/lookup").getRemaining();
				}

				result.addAll(userRes.lookupUsers(Arrays.copyOfRange(
						screenNames, i * 100,
						Math.min(screenNames.length, (i + 1) * 100))));

			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}
}
