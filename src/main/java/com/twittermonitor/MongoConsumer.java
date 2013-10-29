package com.twittermonitor;

import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.util.JSON;

import twitter4j.Status;
import twitter4j.json.DataObjectFactory;

public class MongoConsumer extends Thread {
	private LinkedBlockingQueue<String> sharedQueue;

	public MongoConsumer(LinkedBlockingQueue<String> sharedQueue) {
		this.sharedQueue = sharedQueue;
	}

	public void run() {
		MongoClient mongoClient;
		try {
			mongoClient = new MongoClient( "localhost" , 27017 );
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		DB db = mongoClient.getDB( "twitterdb" );
		db.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
		DBCollection statusCollection = db.getCollection("statuses");
		try {
			while (true) {
				String statJson = sharedQueue.take();
				DBObject dbobj = (DBObject)JSON.parse(statJson);
				statusCollection.insert(dbobj);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			mongoClient.close();
		}
	}

}
