package com.twittermonitor;

public class ThreadStatusObserver  {
	private Thread monitor;
	private Thread userInputThread;
	
	public ThreadStatusObserver(Thread monitor, Thread userInputThread) {
		this.monitor = monitor;
		this.userInputThread = userInputThread;
	}

	protected synchronized void threadError(Thread t) {
		monitor.interrupt();
	}
	
	protected synchronized void threadSuccess(Thread t){
		if(t.getId() == userInputThread.getId()){
			monitor.interrupt();
		}
	}

}
