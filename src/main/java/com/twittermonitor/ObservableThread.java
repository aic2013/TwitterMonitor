package com.twittermonitor;

import java.util.*;

public class ObservableThread extends Thread {
	protected List<ThreadStatusObserver> observers = new ArrayList<>();

	public void registerObserver(ThreadStatusObserver observer) {
		synchronized (observers) {
			observers.add(observer);
		}
	}

	public void fireThreadSuccess() {
		synchronized (observers) {
			for (ThreadStatusObserver obs : observers) {
				obs.threadSuccess(Thread.currentThread());
			}
		}
	}

	public void fireThreadError() {
		synchronized (observers) {
			for (ThreadStatusObserver obs : observers) {
				obs.threadError(Thread.currentThread());
			}
		}
	}

}
