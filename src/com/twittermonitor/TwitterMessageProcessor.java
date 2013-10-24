package com.twittermonitor;

import java.util.concurrent.BlockingQueue;

@Deprecated
public class TwitterMessageProcessor extends Thread {
	private BlockingQueue<String> messageBuffer;

	public TwitterMessageProcessor(BlockingQueue<String> messageBuffer) {
		super();
		this.messageBuffer = messageBuffer;
	}
	
	public void run(){
		try{
			while(true){
				String msg = messageBuffer.take();
				System.out.println(msg);
				
				
			}
		}catch(InterruptedException e){
			System.out.println("TwitterMessageProcessor exits");
		}
	}
}
