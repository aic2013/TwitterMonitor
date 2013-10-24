package com.twittermonitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import twitter4j.Location;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Trend;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.api.TrendsResources;

public class TrendGenerator {
	private LinkedBlockingQueue<Trend> sharedQueue;

	public TrendGenerator(LinkedBlockingQueue<Trend> sharedQueue)
			throws TwitterException {
		this.sharedQueue = sharedQueue;
		new Timer("twitterGenTimer").scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				try {
					TrendGenerator.this.updateTrends();
				} catch (TwitterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, 0, 60 * 60 * 1000);
	}

	public void updateTrends() throws TwitterException {
		Twitter twitter = TwitterFactory.getSingleton();
		Map<String, RateLimitStatus> curTrendMethodLimits = twitter
				.getRateLimitStatus("trends");
		TrendsResources trendsRes = twitter.trends();
		int remainingCalls = twitter
				.getRateLimitStatus("trends")
				.get("/trends/place")
				.getRemaining();
		ResponseList<Location> trendLocations = trendsRes.getAvailableTrends();

		try {
			for (Location trendLocation : trendLocations) {
				while(remainingCalls <= 0) {
					int resetTime = twitter
							.getRateLimitStatus("trends")
							.get("/trends/place")
							.getSecondsUntilReset();
					Thread.sleep((resetTime + 5) * 1000);
					remainingCalls = twitter
							.getRateLimitStatus("trends")
							.get("/trends/place")
							.getRemaining();
					
				}
				Trends trends = trendsRes.getPlaceTrends(trendLocation
						.getWoeid());
				remainingCalls--;
				for (Trend t : trends.getTrends())
					sharedQueue.put(t);

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
