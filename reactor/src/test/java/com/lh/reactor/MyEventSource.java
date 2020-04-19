package com.lh.reactor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on 2020/4/19.
 *
 * @author hao
 */
public class MyEventSource {
	private List<MyEventListener> listeners;

	public MyEventSource() {
		this.listeners = new ArrayList<>();
	}

	public void register(MyEventListener listener) {    // 1
		listeners.add(listener);
	}

	public void newEvent(MyEvent event) {
		for (MyEventListener listener :
				listeners) {
			listener.onNewEvent(event);     // 2
		}
	}

	public void eventStopped() {
		for (MyEventListener listener :
				listeners) {
			listener.onEventStopped();      // 3
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MyEvent {   // 4
		private Date timeStemp;
		private String message;
	}
}
