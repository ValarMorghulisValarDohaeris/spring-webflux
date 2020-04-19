package com.lh.reactor;

import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 2020/4/19.
 *
 * @author hao
 */
public class ReactorTest2
{
	@Test
	public void testGenerate1() {
		final AtomicInteger count = new AtomicInteger(1);   // 1
		Flux.generate(sink -> {
			sink.next(count.get() + " : " + new Date());   // 向“池子”放自定义的数据；
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (count.getAndIncrement() >= 5) {
				sink.complete();     // 告诉generate方法，自定义数据已发完；
			}
		}).subscribe(System.out::println);  // 4
	}

	@Test
	public void testGenerate2() {
		Flux.generate(
				() -> 1,    // 初始化状态值
				(count, sink) -> {      // 2
					sink.next(count + " : " + new Date());
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (count >= 5) {
						sink.complete();
					}
					return count + 1;   // 每次循环都要返回新的状态值给下次使用
				}).subscribe(System.out::println);
	}

	@Test
	public void testGenerator3()
	{
		Flux.generate(
				() -> 1,
				(count, sink) -> {
					sink.next(count + " : " + new Date());
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (count >= 5) {
						sink.complete();
					}
					return count + 1;
				}, System.out::println)     // 最后将count值打印出来。如果 state 使用了数据库连接或者其他需要进行清理的资源，这个 Consumer lambda 可以用来在最后完成资源清理任务。
				.subscribe(System.out::println);
	}

	/**
	 * create是一个更高级的创建Flux的方法，其生成数据流的方式既可以是同步的，也可以是异步的，并且还可以每次发出多个元素。
	 * create用到了FluxSink，后者同样提供 next，error 和 complete 等方法。 与generate不同的是，create不需要状态值，
	 * 		另一方面，它可以在回调中触发多个事件（即使事件是发生在未来的某个时间）。
	 *
	 * create 常用的场景就是将现有的 API 转为响应式，比如监听器的异步方法。
	 * @throws InterruptedException
	 */
	@Test
	public void testCreate1() throws InterruptedException {
		MyEventSource eventSource = new MyEventSource();    // 1
		Flux.create(sink -> {                                 //如果create方法换成generate方法，则会报出异常。因为generate方法不支持异步的方式

			eventSource.register(new MyEventListener() {    // 2
						@Override
						public void onNewEvent(MyEventSource.MyEvent event) {
							sink.next(event);       // 3
						}

						@Override
						public void onEventStopped() {
							sink.complete();        // 4
						}
					});
				}
		).subscribe(System.out::println);       // 5

		for (int i = 0; i < 20; i++) {  // 6
			Random random = new Random();
			TimeUnit.MILLISECONDS.sleep(random.nextInt(1000));
			eventSource.newEvent(new MyEventSource.MyEvent(new Date(), "Event-" + i));
		}
		eventSource.eventStopped(); // 7
	}

	/**
	 * FluxSink还有onRequest方法，这个方法可以用来响应下游订阅者的请求事件。
	 * 从而不仅可以像上一个例子那样，上游在数据就绪的时候将其推送到下游，同时下游也可以从上游拉取已经就绪的数据。
	 * 这是一种推送/拉取混合的模式
	 */
	@Test
	public void testCreate2()
	{
//		Flux<String> bridge = Flux.create(sink -> {
//			myMessageProcessor.register(
//					new MyMessageListener<String>() {
//
//						public void onMessage(List<String> messages) {
//							for(String s : messages) {
//								sink.next(s);   // push方式，主动向下游发出数据；
//							}
//						}
//					});
//			sink.onRequest(n -> {   // 在下游发出请求时被调用
//				List<String> messages = myMessageProcessor.request(n);  // 应下游的请求，查询是否有可用的message
//				for(String s : message) {
//					sink.next(s);
//				}
//			});
//        ...
//		}
	}
}
