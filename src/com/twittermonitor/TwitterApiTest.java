package com.twittermonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import twitter4j.*;
import twitter4j.api.TrendsResources;
import twitter4j.json.DataObjectFactory;

public class TwitterApiTest {
	public static void main(String[] args) throws TwitterException {

		/*
		 * Example trend consumer running in a separate thread - not needed for
		 * streaming
		 */
		Thread trendConsumer = new Thread() {
			public void run() {
				LinkedBlockingQueue<Trend> trendQueue = new LinkedBlockingQueue<>();
				TrendGenerator trendGen;
				try {
					trendGen = new TrendGenerator(trendQueue);
				} catch (TwitterException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return;
				}
				trendGen.generate();

				List<Trend> receivedTrends = new ArrayList<>();
				try {
					while (true) {
						Trend trend = trendQueue.take();
						System.out.println(trend.getName());
						receivedTrends.add(trend);
					}
				} catch (InterruptedException e) {
					System.out.println("Stopping generator");
					trendGen.stop();
				}

				File cache = new File("trends.list");
				ObjectOutputStream oos = null;
				try {
					oos = new ObjectOutputStream(new FileOutputStream(cache));
					oos.writeObject(receivedTrends);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					if (oos != null) {
						try {
							oos.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

				System.out.println("Trend Consumer ends");

			};
		};
		// trendConsumer.start();

		/* load initial user screen names */
		final String resDirPrefix = "resource\\";
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
		List<User> initUsers = twitterAcc
				.lookupUsersByScreenName(initUserScreenNames
						.toArray(new String[0]));
		
		/* eliminate duplicate user IDs */
		Set<Long> followIdsSet = new HashSet<>();
		for (int i = 0; i < initUsers.size(); i++) {
			followIdsSet.add(initUsers.get(i).getId());
		}
		
		/* copy remaining IDs to array */
		long[] followIds = new long[followIdsSet.size()];
		int followIdsIndex = 0;
		for(Long l : followIdsSet){
			followIds[followIdsIndex] = l;
			followIdsIndex++;
		}

		/* build stream filter from user IDs */
		FilterQuery filterQuery = new FilterQuery(followIds);
		filterQuery.language(new String[] { "en" });
		LinkedBlockingQueue<String> statusQueue = new LinkedBlockingQueue<>();
		MongoConsumer mongoConsumer = new MongoConsumer(statusQueue);
		mongoConsumer.start();

		TwitterStream stream = TwitterStreamFactory.getSingleton();
		stream.addListener(new StatusProducer(statusQueue));
		stream.filter(filterQuery);

		try {
			new BufferedReader(new InputStreamReader(System.in)).readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		trendConsumer.interrupt();
		mongoConsumer.interrupt();
		stream.shutdown();
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

		br = new BufferedReader(new InputStreamReader(new FileInputStream(
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
}
