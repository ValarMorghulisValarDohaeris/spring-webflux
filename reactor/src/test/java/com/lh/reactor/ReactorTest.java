package com.lh.reactor;

import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created on 2020/4/18.
 *
 * @author hao
 */
public class ReactorTest
{
	/**
	 * Flux和Mono还提供了多个subscribe方法的变体
	 */
	@Test
	public void testSubscribe()
	{
		Flux.just(1, 2, 3, 4, 5, 6).subscribe(
				System.out::println,
				System.err::println,
				() -> System.out.println("Completed!"));
		System.out.println("----------------------");

		//打印错误信号，没有输出Completed!表明没有发出完成信号
		Mono.error(new Exception("some error")).subscribe(
				System.out::println,
				System.err::println,
				() -> System.out.println("Completed!")
		);
		System.out.println("----------------------");

	}

	/**
	 *  测试与调试
	 */
	@Test
	public void testViaStepVerifier() {
		StepVerifier.create(generateFluxFrom1To6())
				.expectNext(1, 2, 3, 4, 5, 6)//测试下一个期望的数据元素
				.expectComplete()//用于测试下一个元素是否为完成信号
				.verify();
		StepVerifier.create(generateMonoWithError())
				.expectErrorMessage("some error")//校验下一个元素是否为错误信号
				.verify();

	}
	private Flux<Integer> generateFluxFrom1To6() {
		return Flux.just(1, 2, 3, 4, 5, 6);
	}
	private Mono<Integer> generateMonoWithError() {
		return Mono.error(new Exception("some error"));
	}


	@Test
	public void testMap()
	{
		StepVerifier.create(Flux.range(1, 6)    // 用于生成从“1”开始的，自增为1的“6”个整型数据；
				.map(i -> i * i))   // 2
				.expectNext(1, 4, 9, 16, 25, 36)    //3
				.expectComplete();  // 4
	}

	@Test
	public void testFlatMap()
	{
		StepVerifier.create(
				Flux.just("flux", "mono")
						.flatMap(s -> Flux.fromArray(s.split("\\s*"))   // 对于每一个字符串s，将其拆分为包含一个字符的字符串流；
								.delayElements(Duration.ofMillis(100))) // 对每个元素延迟100ms；
						.doOnNext(System.out::print)) // 打印结果为mfolnuox，原因在于各个拆分后的小字符串都是间隔100ms发出的，因此会交叉。
				.expectNextCount(8) // 4
				.verifyComplete();

		 Flux.just(1, 2)
				.map(i -> Mono.just(i * i));
	}

	@Test
	public void testFilter()
	{
		StepVerifier.create(Flux.range(1, 6)
				.filter(i -> i % 2 == 1)    // 1
				.map(i -> i * i))
				.expectNext(1, 9, 25)   // 2
				.verifyComplete();
	}

	@Test
	public void testSimpleOperators() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);  // 定义一个CountDownLatch，初始为1，则会等待执行1次countDown方法后结束，不使用它的话，测试方法所在的线程会直接返回而不会等待数据流发出完毕；
		Flux.zip(
				getZipDescFlux(),
				Flux.interval(Duration.ofMillis(200)))  // 使用Flux.interval声明一个每200ms发出一个元素的long数据流；因为zip操作是一对一的，故而将其与字符串流zip之后，字符串流也将具有同样的速度；
				.subscribe(t -> System.out.println(t.getT1()), null, countDownLatch::countDown);    //zip之后的流中元素类型为Tuple2，使用getT1方法拿到字符串流的元素；定义完成信号的处理为countDown;
		countDownLatch.await(10, TimeUnit.SECONDS);     // countDownLatch.await(10, TimeUnit.SECONDS)会等待countDown倒数至0，最多等待10秒钟。
	}

	private Flux<String> getZipDescFlux() {
		String desc = "Zip two sources together, that is to say wait for all the sources to emit one element and combine these elements once into a Tuple2.";
		return Flux.fromArray(desc.split("\\s+"));  // 1
	}

	/**
	 * 将同步的阻塞调用变为异步的
	 * @throws InterruptedException
	 */
	@Test
	public void testSyncToAsync() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Mono.fromCallable(() -> getStringSync())    // 1
				.subscribeOn(Schedulers.elastic())  // 2
				.subscribe(System.out::println, null, countDownLatch::countDown);
		countDownLatch.await(10, TimeUnit.SECONDS);
	}

	private String getStringSync() {
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "Hello, Reactor!";
	}

	/**
	 * 测试publishOn和subscribeOn对线程的切换调度
	 * @throws InterruptedException
	 */
	@Test
	public void testPublishOnAndSubscribeOn() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Flux.just(1, 2)
				.map(i -> {
					System.out.println(Thread.currentThread());
					return i+i;
				})
				.publishOn(Schedulers.elastic()).filter(i -> {
					System.out.println(Thread.currentThread());
					return i > 1;
				})
				.publishOn(Schedulers.parallel()).map(i -> {
					System.out.println(Thread.currentThread());
					return i+1;
				})
				.subscribeOn(Schedulers.single()).subscribe(System.out::println,
				System.err::println,
				countDownLatch::countDown);
		countDownLatch.await(10, TimeUnit.SECONDS);
	}

	/**
	 * 捕获并返回一个静态的缺省值
	 */
	@Test
	public void testErrorReturn()
	{
		Flux.range(1, 6)
				.map(i -> 10/(i-3))
				.onErrorReturn(0)   // 1
				.map(i -> i*i)
				.subscribe(System.out::println, System.err::println);
	}

	/**
	 * 捕获并执行一个异常处理方法或计算一个候补值来顶替
	 */
	@Test
	public void testErrorResume()
	{
		Flux.range(1, 6)
				.map(i -> 10/(i-3))
				.onErrorResume(e -> Mono.just(new Random().nextInt(6))) // 提供新的数据流
				.map(i -> i*i)
				.subscribe(System.out::println, System.err::println);
	}

	/**
	 * 捕获，并再包装为某一个业务相关的异常，然后再抛出业务异常
	 */
	public void testErrorMap()
	{
//		Flux.just("timeout1")
//				.flatMap(k -> callExternalService(k))   // 调用外部服务；
//				.onErrorMap(original -> new BusinessException("SLA exceeded", original)); // 如果外部服务异常，将其包装为业务相关的异常后再次抛出。

//		Flux.just("timeout1")
//				.flatMap(k -> callExternalService(k))
//				.onErrorResume(original -> Flux.error(
//						new BusinessException("SLA exceeded", original)
//				);
	}

	/**
	 * 捕获，记录错误日志，然后继续抛出
	 */
	public void testDoOnError()
	{
//		Flux.just(endpoint1, endpoint2)
//				.flatMap(k -> callExternalService(k))
//				.doOnError(e -> {   // 1
//					log("uh oh, falling back, service failed for key " + k);    // 2
//				})
//				.onErrorResume(e -> getFromCache(k));
	}

	public void testUsing()
	{
//		Flux.using(
//				() -> getResource(),    // 第一个参数获取资源；
//				resource -> Flux.just(resource.getAll()),   // 第二个参数利用资源生成数据流；
//				MyResource::clean   // 第三个参数最终清理资源。
//		);
	}

	/**
	 * doFinally在序列终止（无论是 onComplete、onError还是取消）的时候被执行，
	 * 并且能够判断是什么类型的终止事件（完成、错误还是取消），以便进行针对性的清理
	 */
	@Test
	public void testFinally()
	{
		LongAdder statsCancel = new LongAdder();    // 1

		Flux<String> flux =
				Flux.just("foo", "bar")
						.doFinally(type -> {
							if (type == SignalType.CANCEL)  // 2
								statsCancel.increment();  // 3
						})
						.take(1);   // 在发出1个元素后取消流。
		flux.subscribe(System.out::println,
				System.err::println,
				() -> System.out.println("Completed!"));
	}

	/**
	 * retry对于上游Flux是采取的重订阅（re-subscribing）的方式，因此重试之后实际上已经一个不同的序列了， 发出错误信号的序列仍然是终止了的
	 * @throws InterruptedException
	 */
	@Test
	public void testRetry() throws InterruptedException {
		Flux.range(1, 6)
				.map(i -> 10 / (3 - i))
				.retry(1)
				.subscribe(System.out::println, System.err::println);
//		Thread.sleep(100);  // 确保序列执行完
	}

	/**
	 * 通过自定义具有流量控制能力的Subscriber进行订阅。Reactor提供了一个BaseSubscriber，我们可以通过扩展它来定义自己的Subscriber。
	 */
	@Test
	public void testBackPressure()
	{
		Flux.range(1, 6)    // 1
				.doOnRequest(n -> System.out.println("Request " + n + " values..."))    // 每次request的时候打印request个数；
				.subscribe(new BaseSubscriber<Integer>() {  // 3
					@Override
					protected void hookOnSubscribe(Subscription subscription) { // 定义在订阅的时候执行的操作
						System.out.println("Subscribed and make a request...");
						request(1); // 向上游请求1个元素
					}

					@Override
					protected void hookOnNext(Integer value) {  // 定义每次在收到一个元素的时候的操作
						try {
							TimeUnit.SECONDS.sleep(1);  // 7
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						System.out.println("Get value [" + value + "]");    // 8
						request(1); // 9
					}
				});
	}
}
