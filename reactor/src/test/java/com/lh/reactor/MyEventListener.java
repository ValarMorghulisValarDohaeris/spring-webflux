package com.lh.reactor;

/**
 * Created on 2020/4/19.
 *
 * @author hao
 */
public interface MyEventListener {
	void onNewEvent(MyEventSource.MyEvent event);
	void onEventStopped();
}
