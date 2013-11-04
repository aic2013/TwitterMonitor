package com.twittermonitor;

import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.util.JSON;

import twitter4j.Status;
import twitter4j.json.DataObjectFactory;

public class MongoConsumer extends ObservableThread {
	private LinkedBlockingQueue<String> sharedQueue;
	private MongoClient mongoClient;

	public MongoConsumer(MongoClient mongoClient, LinkedBlockingQueue<String> sharedQueue) {
		this.sharedQueue = sharedQueue;
		this.mongoClient = mongoClient;
	}

	public void run() {
		DB db = mongoClient.getDB( "twitterdb" );
		db.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
		DBCollection statusCollection = db.getCollection("statuses");
		try {
			while (true) {
				String statJson = sharedQueue.take();
				DBObject dbobj = (DBObject)JSON.parse(statJson);
				statusCollection.insert(dbobj);
			}
		} catch (InterruptedException | MongoException e) {
			e.printStackTrace();
			fireThreadError();
		} finally{
			mongoClient.close();
		}
	}

}
