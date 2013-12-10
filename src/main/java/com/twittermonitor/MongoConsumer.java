package com.twittermonitor;

import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.bson.BSONException;

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
	private final CharsetEncoder asciiEncoder = Charset.forName(
			"US-ASCII").newEncoder();
	private int bsonExceptionCount = 0;

	public int getBsonExceptionCount() {
		return bsonExceptionCount;
	}

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
				try{
					String statJson = sharedQueue.take();
					DBObject dbobj = (DBObject)JSON.parse(statJson);
					/* consider ascii tweets only */
					if (!asciiEncoder.canEncode(dbobj.get("text").toString())) {
						continue;
					}
					
					statusCollection.insert(dbobj);
				}catch(BSONException bsone){
					bsonExceptionCount++;
					bsone.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fireThreadError();
		} finally{
			mongoClient.close();
		}
	}

}
