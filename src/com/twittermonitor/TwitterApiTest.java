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

public class TwitterApiTest {
	public static void main(String[] args) throws TwitterException {
		
		/* Example trend consumer running in a separate thread */
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
		List<String> initUserScreenNames = new ArrayList<>();
		/* load initial users with high follower rank */
		try {
			initUserScreenNames
					.addAll(loadUserScreenNames("resource\\follower_rank.csv"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* load initial users with high retweet rank */
		try {
			initUserScreenNames
					.addAll(loadUserScreenNames("resource\\retweet_rank.csv"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		TwitterAccess twitterAcc = new CachedTwitterAccess("twittercache");
		List<User> initUsers = twitterAcc.lookupUsersByScreenName(initUserScreenNames.toArray(new String[0]));
		long[] followIds = new long[initUsers.size()];

		for(int i = 0; i < followIds.length; i++){
			followIds[i] = initUsers.get(i).getId();
		}
		
		/* build stream filter from user IDs*/
		FilterQuery filterQuery = new FilterQuery(followIds);

		TwitterStream stream = TwitterStreamFactory.getSingleton();
		stream.addListener(new StatusListener() {

			@Override
			public void onException(Exception arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onTrackLimitationNotice(int arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStatus(Status status) {
				System.out.println(status.getText());
			}

			@Override
			public void onStallWarning(StallWarning arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onScrubGeo(long arg0, long arg1) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onDeletionNotice(StatusDeletionNotice arg0) {
				// TODO Auto-generated method stub

			}
		});
		stream.filter(filterQuery);
		
		try {
			new BufferedReader(new InputStreamReader(System.in)).readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		trendConsumer.interrupt();
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
