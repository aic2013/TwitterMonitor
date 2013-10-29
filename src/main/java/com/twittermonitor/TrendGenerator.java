package com.twittermonitor;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import twitter4j.Location;
import twitter4j.ResponseList;
import twitter4j.Trend;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.api.TrendsResources;

/**
 * TrendGenerator will usually deliver duplicate Trends!
 * 
 * TODO: Change to user ScheduledExecutorService in order to be able
 * to interrupt running tasks.
 * @author Moritz Becker
 *
 */
public class TrendGenerator {
	private LinkedBlockingQueue<Trend> sharedQueue;
	private Timer updateTimer;

	public TrendGenerator(LinkedBlockingQueue<Trend> sharedQueue)
			throws TwitterException {
		this.sharedQueue = sharedQueue;
	}

	public void generate() {
		updateTimer = new Timer("trendGenTimer");
		updateTimer.schedule(new UpdateTask(), 0);
	}
	
	public void stop(){
		updateTimer.cancel();
		updateTimer.purge();
	}
	

	class UpdateTask extends TimerTask {
		public void run() {
			try {
				Calendar start = Calendar.getInstance();
				updateTrends();
				Calendar end = Calendar.getInstance();
				/*
				 * If the last update took less than an hour, wait for the hour
				 * to be completed and then do the next update.
				 * Else, if the update took longer than an hour, the next update takes
				 * place immediately.
				 */
				scheduleNextUpdate(Math.max(0, 
						60*60*1000-(end.getTimeInMillis() - start.getTimeInMillis())
						));
			} catch (InterruptedException | TwitterException e) {
				e.printStackTrace();
			}
		}

		private void scheduleNextUpdate(long delay) {
			/* schedule next update */
			updateTimer.schedule(this, delay);
		}

		private void updateTrends() throws TwitterException,
				InterruptedException {

			Twitter twitter = TwitterFactory.getSingleton();
			TrendsResources trendsRes = twitter.trends();
			int remainingCalls = twitter.getRateLimitStatus("trends")
					.get("/trends/place").getRemaining();
			ResponseList<Location> trendLocations = trendsRes
					.getAvailableTrends();

			for (Location trendLocation : trendLocations) {
				while (remainingCalls <= 0) {
					int resetTime = twitter.getRateLimitStatus("trends")
							.get("/trends/place").getSecondsUntilReset();
					Thread.sleep((resetTime + 5) * 1000);
					remainingCalls = twitter.getRateLimitStatus("trends")
							.get("/trends/place").getRemaining();

				}
				Trends trends = trendsRes.getPlaceTrends(trendLocation
						.getWoeid());
				remainingCalls--;
				for (Trend t : trends.getTrends())
					sharedQueue.put(t);
			}
		}
	}

}
