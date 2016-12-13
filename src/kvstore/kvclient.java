package kvstore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class kvclient {

	public static final String EMPTY_STRING = "";
	public static final String FIXED_VAL = "0";
	public static final String GET = "get";
	public static final String SET = "set";
	public static final String DEL = "del";
	private static final int NUM_OPERATIONS = 500;
	private static final int MYTHREADS = 4;
	private static final String DEFAULT_VAL = "0";
	public static AtomicInteger sequenceNumber = new AtomicInteger(0);
	static List<Log> loglist = Collections.synchronizedList(new ArrayList<Log>());
	public static AtomicInteger valueToWrite = new AtomicInteger(100);
	public static ConcurrentHashMap<String, String> writtenVaues = new ConcurrentHashMap<String, String>();
	public static volatile boolean staleValueReturned = false;

	/**
	 * @return Sequence Server
	 */
	static int getNext() {
		return sequenceNumber.incrementAndGet();
	}

	/**
	 * @return Function to get next value to write
	 */
	static String getNextValToWrite() {
		return String.valueOf(valueToWrite.incrementAndGet());
	}

	/**
	 * Function to print generalized error.
	 */
	static void printError() {
		//System.out.println("Bad command!");
		System.exit(2);
	}

	/**
	 * Method to print insufficient number of arguments exception.
	 */
	static void printInsuffNoOfArgs() {
		//System.out.println("Insufficient Number of Arguments!");
		System.exit(2);
	}

	/**
	 * Function to perform analysis
	 */
	static void performAnalysis() {

		if (staleValueReturned) {
			//System.out.println("Returned value was never written before. Exit with 1");
			System.exit(1);
		}
		Graph graph = new Graph(loglist.size());

		Collections.sort(loglist, new Comparator<Log>() {

			@Override
			public int compare(Log o1, Log o2) {
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
			log.setUniqueIdentifier(loglist.get(i).getUniqueIdentifier());
			endingList.add(log);
		}

		Collections.sort(endingList, new Comparator<Log>() {

			@Override
			public int compare(Log o1, Log o2) {
				return o1.getEndtime() < o2.getEndtime() ? -1 : o1.getEndtime() == o2.getEndtime() ? 0 : 1;
			}
		});

		// System.out.println("List A");
		// for (int i = 0; i < loglist.size(); i++) {
		// System.out.println("UID = " + loglist.get(i).getUniqueIdentifier() +
		// " : Start = "
		// + loglist.get(i).getStarttime() + " : Operation = " +
		// loglist.get(i).getOperation() + " : Val = "
		// + loglist.get(i).getValue());
		// }

		// System.out.println("List B");
		// for (int i = 0; i < endingList.size(); i++) {
		// System.out.println(
		// "UID = " + endingList.get(i).getUniqueIdentifier() + " : End = " +
		// endingList.get(i).getEndtime());
		// }

		//System.out.println("Adding time edges");
		// Add time edges
		int time = 0;
		for (int i = 0; i < loglist.size(); i++) {
			time = Integer.MIN_VALUE;
			for (int j = 0; j < endingList.size()
					&& endingList.get(j).getEndtime() < loglist.get(i).getStarttime(); j++) {
				if (time < endingList.get(j).getEndtime()) {
					// System.out.println("Adding time edge " +
					// endingList.get(j).getUniqueIdentifier() + " -> " +
					// loglist.get(i).getUniqueIdentifier());
					graph.addEdge(endingList.get(j).getUniqueIdentifier(), loglist.get(i).getUniqueIdentifier());
					time = Math.max(time, endingList.get(j).getStarttime());
				} else
					break;
			}
		}
		//System.out.println("Time edges added");

		//System.out.println("Adding data edges");
		// Add data edges
		for (int i = 0; i < loglist.size(); i++) {
			if (loglist.get(i).getOperation() == SET) {
				String value_written = loglist.get(i).getValue();
				for (int j = 0; j < loglist.size(); j++) {
					if (loglist.get(j).getOperation() == GET) {
						if (loglist.get(j).getValue().equals(value_written)) {
							// System.out.println("Adding data edge " +
							// loglist.get(i).getUniqueIdentifier() + " -> " +
							// loglist.get(j).getUniqueIdentifier());
							graph.addEdge(loglist.get(i).getUniqueIdentifier(), loglist.get(j).getUniqueIdentifier());
							loglist.get(j).setDictatingWrite(loglist.get(i).getUniqueIdentifier());
						}
					}
				}
			}
		}
		//System.out.println("Data edges added");

		//System.out.println("Adding hybrid edges");
		// Add hybrid edges
		for (int i = 0; i < loglist.size(); i++) {
			if (loglist.get(i).getOperation() == SET) {
				for (int j = 0; j < loglist.size(); j++) {
					if (loglist.get(j).getOperation() == GET) {
						if (graph.isReachable(loglist.get(i).getUniqueIdentifier(),
								loglist.get(j).getUniqueIdentifier())) {
							if (loglist.get(i).getUniqueIdentifier() != loglist.get(j).getDictatingWrite()) {
								// System.out.println("Adding hybrid edge " +
								// loglist.get(i).getUniqueIdentifier() + " -> "
								// + loglist.get(j).getDictatingWrite());
								graph.addEdge(loglist.get(i).getUniqueIdentifier(), loglist.get(j).getDictatingWrite());
							}
						}
					}
				}
			}
		}
		//System.out.println("Hybrid edges added");

		// Print edges
		// for (int k = 0; k < graph.getList().length; k++) {
		// System.out.println("Operation # = " + k + " Links = " +
		// graph.getList()[k].size());
		// for (int m = 0; m < graph.getList()[k].size(); m++) {
		// System.out.println(" -> " + graph.getList()[k].get(m));
		// }
		// }

		//System.out.println("Checking if graph is acyclic..");
		if (graph.isCyclic()) {
			//System.out.println("Graph contains cycle(s) - Exit with 1");
			System.exit(1);
		} else {
			//System.out.println("Graph does not contain cycle(s) - Exit with 0");
			System.exit(0);
		}

	}

	/**
	 * @param args
	 * 
	 *            Main Function
	 */
	public static void main(String[] args) {

		TTransport transport;
		List<Future<Boolean>> list = null;
		try {
			// Server Name
			String server = args[0];

			// Get the server address
			String[] address = args[1].split(":");

			String ip = address[0];
			String port = address[1];

			transport = new TSocket(ip, Integer.parseInt(port));
			try {
				transport.open();
			} catch (TTransportException e1) {
				e1.printStackTrace();
				System.exit(2);
			}
			TProtocol protocol = new TBinaryProtocol(transport);
			KVStore.Client client = new KVStore.Client(protocol);
			try {
				client.kvset("X", DEFAULT_VAL);
			} catch (TException e1) {
				e1.printStackTrace();
				System.exit(2);
			}
			//System.out.println("Default value set");
			transport.close();
			writtenVaues.put(DEFAULT_VAL, FIXED_VAL);
			int i = 0;
			ExecutorService executor = Executors.newFixedThreadPool(MYTHREADS);

			Collection<MyRunnable> array = new ArrayList<MyRunnable>();
			// ExecutorService executor1 = Executors.newSingleThreadExecutor();
			for (i = 0; i < NUM_OPERATIONS; i++) {
				array.add(new MyRunnable("localhost", 9090, i));
			}
			try {
				list = executor.invokeAll(array);
				// Future<Boolean> result = executor1.submit(new
				// MyRunnable("localhost", 9090, i++));
				// result.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(2);
			} finally {
				executor.shutdown();
				// executor1.shutdown();
				// for (i = 0; i < list.size(); i++) {
				// System.out.println(list.get(i).isDone());
				// }
			}

			// System.out.println("Number of logs = " + loglist.size());
			// for (i = 0; i < loglist.size(); i++) {
			// System.out.println(loglist.get(i).toString());
			// }

			performAnalysis();
		} catch (ArrayIndexOutOfBoundsException ae) {
			printInsuffNoOfArgs();
		} catch (Exception e) {
			printError();
		}
	}

	public static class MyRunnable implements Callable<Boolean> {
		Log log = null;
		String ip = null;
		int port = 0;
		int counter = 0;
		private static final String KEY = "X";

		public MyRunnable(String ip, int port, int counter) {
			this.ip = ip;
			this.port = port;
			this.counter = counter;
			log = new Log();
		}

		MyRunnable() {
		}

		/**
		 * @param result
		 * @param log
		 * 
		 *            Function to process result
		 */
		static void processResult(Result result, Log log) {
			String errorText = EMPTY_STRING;
			int sequenceno = -1;
			if (null != result) {
				ErrorCode errorCode = result.getError();
				switch (errorCode) {
				case kSuccess:
					String value = result.getValue();
					errorText = result.getErrortext();
					if (log.getOperation() == GET) {
						sequenceno = getNext();
						// System.out.println("Success - Operation # " +
						// log.getUniqueIdentifier() + " - " +
						// log.getOperation() + " value -" + value + " Sequence
						// number " + sequenceno);
						log.setEndtime(sequenceno);
						log.setValue(value);
						if (!writtenVaues.containsKey(value)) {
							staleValueReturned = true;
							//System.out.println("Returned value was never written before.");
						}
					}
					if (null != errorText && log.getOperation() == SET) {
						// System.err.println(errorText);
						sequenceno = getNext();
						// System.out.println("Success - Operation # " +
						// log.getUniqueIdentifier() + " - " +
						// log.getOperation() + " message " + errorText + "
						// Sequence number " + sequenceno);
						log.setEndtime(sequenceno);

					}
					// System.exit(errorCode.getValue());
					break;

				case kKeyNotFound:
					errorText = result.getErrortext();
					if (null != errorText) {
						sequenceno = getNext();
						// System.out.println("Failure - Operation # " +
						// log.getUniqueIdentifier() + " - " +
						// log.getOperation() + " Error" + errorText
						// + " Sequence number " + sequenceno);
						log.setEndtime(sequenceno);
					}
					// System.exit(errorCode.getValue());
					break;

				case kError:
					errorText = result.getErrortext();
					if (null != errorText) {
						sequenceno = getNext();
						// System.err.println(errorText);
						// System.out.println("Error - Operation # " +
						// log.getUniqueIdentifier() + " Operation - " +
						// log.getOperation() + " Error" + errorText
						// + " Sequence number " + sequenceno);
						log.setEndtime(sequenceno);
					}
					// System.exit(errorCode.getValue());
					break;
				}
			} else {
				//System.out.println("Empty Result");
				System.exit(2);
			}
		}

		@Override
		public Boolean call() {
			try {
				TTransport transport;

				String operation = "";
				String value = "";
				log.setUniqueIdentifier(counter);
				int sequenceno = getNext();

				// MyRunnable mr = new MyRunnable();
				// mr.setCounter(counter);
				double d = Math.random();
				if (d < 0.5) {
					operation = GET;
					// System.out.println("Operation # " + counter + " - " +
					// operation + " key -" + KEY + " Sequence number " +
					// sequenceno);
				} else {
					operation = SET;
					value = getNextValToWrite();
					// System.out.println("Operation # " + counter + " - " +
					// operation + " key -" + KEY + " value -"+ value + "
					// Sequence number " + sequenceno);
					log.setValue(value);
					writtenVaues.put(log.getValue(), FIXED_VAL);
				}

				log.setKey(KEY);
				log.setOperation(operation);
				log.setStarttime(sequenceno);

				// Open a socket to server
				transport = new TSocket(ip, port);
				transport.open();

				TProtocol protocol = new TBinaryProtocol(transport);
				KVStore.Client client = new KVStore.Client(protocol);

				Result res = null;
				switch (operation) {
				case GET:
					res = client.kvget(KEY);
					// processResult(res, log);
					break;
				case SET:
					res = client.kvset(KEY, value);
					// processResult(res, log);
					break;
				case DEL:
					res = client.kvdelete(KEY);
					// processResult(res, log);
					break;
				default:
					break;
				}
				processResult(res, log);
				kvclient.loglist.add(log);
				transport.close();
			}

			catch (TTransportException e) {
				// e.printStackTrace();
				//System.out.println("Server not Running");
				System.exit(2);
			} catch (Exception e) {
				//System.out.println("Error!! Thread Number " + counter);
				System.exit(2);
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
