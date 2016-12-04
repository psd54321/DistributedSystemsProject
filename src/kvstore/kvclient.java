package kvstore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class kvclient {

	public static final String EMPTY_STRING = "";
	public static final String GET = "get";
	public static final String SET = "set";
	public static final String DEL = "del";
	private static final int MYTHREADS = 5;
	public static AtomicInteger sequenceNumber = new AtomicInteger(0);
	static List<Log> loglist = Collections.synchronizedList(new ArrayList<Log>());

	static int getNext() {
		return sequenceNumber.incrementAndGet();
	}
	// /**
	// * Function to print generalized error.
	// */
	// static void printError() {
	// System.out.println("Bad command!!");
	// //System.exit(2);
	// }
	//
	// /**
	// * Method to print insufficient number of arguments exception.
	// */
	// static void printInsuffNoOfArgs() {
	// System.out.println("Insufficient Number of Arguments!!");
	// //System.exit(2);
	// }

	/**
	 * @param result
	 * 
	 *            Function to process result object.
	 */

	static void performAnalysis() {
		Graph graph = new Graph(loglist.size());

		Collections.sort(loglist, new Comparator<Log>() {

			@Override
			public int compare(Log o1, Log o2) {
				// TODO Auto-generated method stub
				return o1.getStarttime() < o2.getStarttime() ? -1 : o1.getStarttime() == o2.getStarttime() ? 0 : 1;
			}
		});

		List<Log> endingList = new ArrayList<Log>();
		for (int i = 0; i < loglist.size(); i++) {
			Log log = new Log();
			log.setEndtime(loglist.get(i).getEndtime());
			log.setStarttime(loglist.get(i).getStarttime());
			log.setKey(loglist.get(i).getKey());
			log.setValue(loglist.get(i).getValue());
			log.setOperation(loglist.get(i).getOperation());
			endingList.add(log);
		}

		Collections.sort(endingList, new Comparator<Log>() {

			@Override
			public int compare(Log o1, Log o2) {
				// TODO Auto-generated method stub
				return o1.getEndtime() < o2.getEndtime() ? -1 : o1.getEndtime() == o2.getEndtime() ? 0 : 1;
			}
		});

		int time = 0;

		for (int i = 0; i < loglist.size(); i++) {
			time = Integer.MIN_VALUE;
			for (int j = 0; j < endingList.size()
					&& endingList.get(j).getEndtime() < loglist.get(i).getStarttime(); j++) {
				if (time < endingList.get(j).getEndtime()) {
					graph.addEdge(endingList.get(j), loglist.get(i));
					time = Math.max(time, endingList.get(j).getStarttime());
				} else
					break;
			}
		}

		for (int k = 0; k < graph.getList().length; k++) {
			System.out.println(k);
			for (int m = 0; m < graph.getList()[k].size(); m++) {
				System.out.println(" -> " + graph.getList()[k].get(m).getIndex());
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int i = 0;
		ExecutorService executor = Executors.newFixedThreadPool(MYTHREADS);

		Collection<MyRunnable> array = new ArrayList<MyRunnable>();
		ExecutorService executor1 = Executors.newSingleThreadExecutor();
		for (i = 0; i < 3; i++) {
			array.add(new MyRunnable("localhost", 9090, i));
		}
		try {
			List<Future<Boolean>> list = executor.invokeAll(array);
			Future<Boolean> result = executor1.submit(new MyRunnable("localhost", 9090, i++));
			result.get();
			// result = executor1.submit(new MyRunnable("localhost", 9090,
			// i++));
			// executor1.submit(new MyRunnable("localhost", 9090, i++));
			// result.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			executor.shutdown();
			executor1.shutdown();
		}
		// loglist.toString();
		for (i = 0; i < loglist.size(); i++) {
			System.out.println(loglist.get(i).toString());
		}
		System.out.println(loglist.size());

		performAnalysis();
	}

	public static class MyRunnable implements Callable<Boolean> {
		Log log = null;
		String ip = null;
		int port = 0;
		int counter = 0;
		public static String[] keyArray = { "A", "B", "C", "D", "E" };

		/**
		 * 
		 */
		public MyRunnable(String ip, int port, int counter) {
			// TODO Auto-generated constructor stub
			this.ip = ip;
			this.port = port;
			this.counter = counter;
			log = new Log();
		}

		MyRunnable() {

		}

		static void processResult(Result result, String method, int counter, Log log) {
			String errorText = EMPTY_STRING;
			int sequenceno = -1;
			if (null != result) {
				ErrorCode errorCode = result.getError();
				switch (errorCode) {
				case kSuccess:
					String value = result.getValue();
					errorText = result.getErrortext();
					if (method == GET) {
						// System.out.println(value);
						sequenceno = getNext();
						System.out.println("Thread " + counter + " Operation - " + method + " value -" + value
								+ " Sequence number " + sequenceno);
						log.setEndtime(sequenceno);
						log.setValue(value);
					}
					if (null != errorText && method == SET) {
						// System.err.println(errorText);
						sequenceno = getNext();
						System.out.println("Thread " + counter + " Operation - " + method + " message " + errorText
								+ " Sequence number " + sequenceno);
						log.setEndtime(sequenceno);
					}
					// System.exit(errorCode.getValue());
					break;

				case kKeyNotFound:
					errorText = result.getErrortext();
					if (null != errorText) {
						sequenceno = getNext();
						// System.err.println(errorText);
						System.out.println("Thread " + counter + " Operation - " + method + " Error" + errorText
								+ " Sequence number " + sequenceno);
						log.setEndtime(sequenceno);
					}
					// System.exit(errorCode.getValue());
					break;

				case kError:
					errorText = result.getErrortext();
					if (null != errorText) {
						sequenceno = getNext();
						// System.err.println(errorText);
						System.out.println("Thread " + counter + " Operation - " + method + " Error" + errorText
								+ " Sequence number " + sequenceno);
						log.setEndtime(sequenceno);
					}
					// System.exit(errorCode.getValue());
					break;
				}
			} else {
				System.out.println("Empty Result");
				// System.exit(2);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public Boolean call() {
			// TODO Auto-generated method stub
			try {
				TTransport transport;

				String operation = "";
				String key = EMPTY_STRING;
				String value = "";
				MyRunnable mr = new MyRunnable();
				mr.setCounter(counter);
				double d = Math.random();
				log.setIndex(counter);
				if (d < 0.5) {
					operation = GET;
					// key = keyArray[ThreadLocalRandom.current().nextInt(0,
					// 5)];
					key = "X";
					int sequenceno = getNext();
					System.out.println("Thread " + counter + " Operation - " + operation + " key -" + key
							+ " Sequence number " + sequenceno);

					log.setKey(key);
					log.setOperation(operation);
					log.setStarttime(sequenceno);
					// log.setValue(value);

				} else {
					operation = SET;
					// key = keyArray[ThreadLocalRandom.current().nextInt(0,
					// 5)];
					key = "X";
					value = String.valueOf((int) Math.floor(d * 100));
					int sequenceno = getNext();
					System.out.println("Thread " + counter + " Operation - " + operation + " key -" + key + "   value -"
							+ value + " Sequence number " + sequenceno);

					log.setKey(key);
					log.setValue(value);
					log.setOperation(operation);
					log.setStarttime(sequenceno);
				}

				// Open a socket to server
				transport = new TSocket(ip, port);
				transport.open();

				TProtocol protocol = new TBinaryProtocol(transport);
				KVStore.Client client = new KVStore.Client(protocol);

				Result res = null;

				// operation = (operation.substring(operation.indexOf("-") + 1,
				// operation.length()));
				switch (operation) {
				case GET:
					res = client.kvget(key);
					processResult(res, GET, mr.getCounter(), log);
					break;
				case SET:
					res = client.kvset(key, value);
					processResult(res, SET, mr.getCounter(), log);
					break;
				case DEL:
					res = client.kvdelete(key);
					processResult(res, DEL, mr.getCounter(), log);
					break;

				default:
					break;
				}
				// System.out.println("Fuck!!!");
				kvclient.loglist.add(log);
				transport.close();
			}

			catch (TTransportException e) {
				// e.printStackTrace();
				System.out.println("Server not Running");
				System.exit(2);
			} catch (Exception e) {
				System.out.println("Error!! Thread Number " + counter);
			}
			return true;
		}

		public int getCounter() {
			return counter;
		}

		public void setCounter(int counter) {
			this.counter = counter;
		}

		public Log getLog() {
			return log;
		}

		public void setLog(Log log) {
			this.log = log;
		}

	}

}
