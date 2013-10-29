package com.twittermonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.Trend;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;

public class TwitterMonitor extends Thread implements StatusListener {
	private Set<Long> followIdsSet = new HashSet<>();
	private final static int maxFollowIds = 5000;
	
	public static void main(String[] args){
		/*
		 * Example trend consumer running in a separate thread - not needed for
		 * streaming
		 */
//		Thread trendConsumer = new Thread() {
//			public void run() {
//				LinkedBlockingQueue<Trend> trendQueue = new LinkedBlockingQueue<>();
//				TrendGenerator trendGen;
//				try {
//					trendGen = new TrendGenerator(trendQueue);
//				} catch (TwitterException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//					return;
//				}
//				trendGen.generate();
//
//				List<Trend> receivedTrends = new ArrayList<>();
//				try {
//					while (true) {
//						Trend trend = trendQueue.take();
//						System.out.println(trend.getName());
//						receivedTrends.add(trend);
//					}
//				} catch (InterruptedException e) {
//					System.out.println("Stopping generator");
//					trendGen.stop();
//				}
//
//				File cache = new File("trends.list");
//				ObjectOutputStream oos = null;
//				try {
//					oos = new ObjectOutputStream(new FileOutputStream(cache));
//					oos.writeObject(receivedTrends);
//
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} finally {
//					if (oos != null) {
//						try {
//							oos.close();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
//				}
//
//				System.out.println("Trend Consumer ends");
//
//			};
//		};
		
		TwitterMonitor monitor = new TwitterMonitor();
		monitor.start();

		try {
			new BufferedReader(new InputStreamReader(System.in)).readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		monitor.interrupt();
	}
	@Override
	public void run() {
		/* load initial user screen names */
		final String resDirPrefix = "";
		List<String> initUserScreenNames = new ArrayList<>();
		try {
			initUserScreenNames.addAll(loadUserScreenNames(resDirPrefix
					+ "follower_rank.csv"));
			initUserScreenNames.addAll(loadUserScreenNames(resDirPrefix
					+ "retweet_rank.csv"));
			initUserScreenNames.addAll(loadUserScreenNames(resDirPrefix
					+ "twittercounter_following.csv"));
			initUserScreenNames.addAll(loadUserScreenNames(resDirPrefix
					+ "tweet_rank_#blogger.csv"));
			initUserScreenNames.addAll(loadUserScreenNames(resDirPrefix
					+ "tweet_rank_#internet.csv"));
			initUserScreenNames.addAll(loadUserScreenNames(resDirPrefix
					+ "tweet_rank_#kultur.csv"));
			initUserScreenNames.addAll(loadUserScreenNames(resDirPrefix
					+ "tweet_rank_#marketing.csv"));
			initUserScreenNames.addAll(loadUserScreenNames(resDirPrefix
					+ "tweet_rank_#medien.csv"));
			initUserScreenNames.addAll(loadUserScreenNames(resDirPrefix
					+ "tweet_rank_#nachrichten.csv"));
			initUserScreenNames.addAll(loadUserScreenNames(resDirPrefix
					+ "tweet_rank_#socialmedia.csv"));
			initUserScreenNames.addAll(loadUserScreenNames(resDirPrefix
					+ "tweet_rank_#twitter.csv"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		TwitterAccess twitterAcc = new CachedTwitterAccess("twittercache");
		List<User> initUsers;
		try {
			initUsers = twitterAcc.lookupUsersByScreenName(initUserScreenNames
					.toArray(new String[0]));
		} catch (TwitterException e) {
			e.printStackTrace();
			return;
		}

		/* eliminate duplicate user IDs */
		for (int i = 0; i < initUsers.size(); i++) {
			followIdsSet.add(initUsers.get(i).getId());
		}

		

		/* build stream filter from user IDs */

		LinkedBlockingQueue<String> statusQueue = new LinkedBlockingQueue<>();
		MongoConsumer mongoConsumer = new MongoConsumer(statusQueue);
		mongoConsumer.start();

		TwitterStream stream = TwitterStreamFactory.getSingleton();
		stream.addListener(new StatusProducer(statusQueue));
		stream.addListener(this);

		try {
			while (true) {
				/* copy remaining IDs to array */
				long[] followIds = new long[followIdsSet.size()];
				int followIdsIndex = 0;
				for (Long l : followIdsSet) {
					followIds[followIdsIndex] = l;
					followIdsIndex++;
				}
				
				FilterQuery filterQuery = new FilterQuery(followIds);
				filterQuery.language(new String[] { "en" });
				stream.filter(filterQuery);
				Thread.sleep(20000);
				stream.shutdown();
			}
		} catch (InterruptedException e) {
			System.out.println("TwitterMonitor exits");
		} finally{
			stream.shutdown();
			mongoConsumer.interrupt();
		}
	}

	/**
	 * 
	 * @param filePath
	 *            Path to semicolon-separated csv-File with a column header that
	 *            contains a column "Screen Name"
	 * @return
	 * @throws IOException
	 */
	private static List<String> loadUserScreenNames(String filePath)
			throws IOException {
		BufferedReader br = null;
		List<String> result = new ArrayList<>();

		br = new BufferedReader(new InputStreamReader(TwitterMonitor.class.getClassLoader().getResourceAsStream(
				filePath)));

		int screenNameColIndex = -1;
		String line;

		/* inspect header */

		line = br.readLine();
		String[] columnNames = line.split(";");
		for (int i = 0; i < columnNames.length; i++) {
			if ("Screen Name".compareTo(columnNames[i].trim()) == 0) {
				screenNameColIndex = i;
				break;
			}
		}

		if (screenNameColIndex == -1) {
			return null;
		}

		while ((line = br.readLine()) != null) {
			result.add(line.split(";")[screenNameColIndex]);
		}

		return result;
	}

	@Override
	public void onException(Exception arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeletionNotice(StatusDeletionNotice arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onScrubGeo(long arg0, long arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStallWarning(StallWarning arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatus(Status arg0) {
		if(followIdsSet.size() < TwitterMonitor.maxFollowIds){
			if(arg0.isRetweeted()){
				followIdsSet.add(arg0.getRetweetedStatus().getUser().getId());
			}
			followIdsSet.add(arg0.getUser().getId());
		}
	}

	@Override
	public void onTrackLimitationNotice(int arg0) {
		// TODO Auto-generated method stub
		
	}
}
