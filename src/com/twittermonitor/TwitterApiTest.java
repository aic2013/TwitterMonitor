package com.twittermonitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import twitter4j.*;
import twitter4j.api.TrendsResources;

public class TwitterApiTest {
	public static void main(String[] args) throws TwitterException {
		TwitterAccess twitterAcc = new CachedTwitterAccess("twittercache");
		List<Trend> allTrends = twitterAcc.getTrends();

		LinkedBlockingQueue<Trend> trendQueue = new LinkedBlockingQueue<>();
		TrendGenerator trendGen = new TrendGenerator(trendQueue);

		try {
			while (true) {
				System.out.println(trendQueue.take());
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}

		List<Trend> receivedTrends = new ArrayList<>();
		for (Trend t : allTrends) {
			System.out.println(t.getName());
			receivedTrends.add(t);
		}
		
		File cache = new File("trends.list");
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(cache));
			oos.writeObject(receivedTrends);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(oos != null){
				try {
					oos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		
		 /* allTrends contains all trending topics */
		 /* use trending topics to construct a list of phrases which are then
		 tracked in the twitter stream
		 * https://dev.twitter.com/docs/streaming-apis/parameters#track
		 */
		
		//
		// StringBuilder trackKeywords = new StringBuilder();
		//
		// for(Trend t : allTrends){
		// String topicName = t.getName();
		// if(topicName.startsWith("#")){
		// if(topicName.length() > 1){
		// trackKeywords.append(t.getName().substring(1));
		// }
		// }else{
		// trackKeywords.append(t.getName());
		// }
		// trackKeywords.append(',');
		// }
		// /* remove last ',' */
		// if(trackKeywords.length() > 0)
		// trackKeywords.deleteCharAt(trackKeywords.length()-1);
		//
		// System.out.println("Tracking parameter:\n" +
		// trackKeywords.toString());
		//
		// twitter.shutdown();

		// TwitterStream stream = TwitterStreamFactory.getSingleton();
		// stream.addListener(new StatusListener() {
		//
		// @Override
		// public void onException(Exception arg0) {
		// // TODO Auto-generated method stub
		//
		// }
		//
		// @Override
		// public void onTrackLimitationNotice(int arg0) {
		// // TODO Auto-generated method stub
		//
		// }
		//
		// @Override
		// public void onStatus(Status status) {
		// System.out.println(status.getText());
		// }
		//
		// @Override
		// public void onStallWarning(StallWarning arg0) {
		// // TODO Auto-generated method stub
		//
		// }
		//
		// @Override
		// public void onScrubGeo(long arg0, long arg1) {
		// // TODO Auto-generated method stub
		//
		// }
		//
		// @Override
		// public void onDeletionNotice(StatusDeletionNotice arg0) {
		// // TODO Auto-generated method stub
		//
		// }
		// });
		// stream.sample();
		// stream.shutdown();
	}
}
