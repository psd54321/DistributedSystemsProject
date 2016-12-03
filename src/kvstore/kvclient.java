package kvstore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
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
	
	static int getNext(){
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ExecutorService executor = Executors.newFixedThreadPool(MYTHREADS);
		Collection<MyRunnable> array = new ArrayList<MyRunnable>();
		for(int i = 0 ;i < 5 ; i++){
			array.add(new MyRunnable("localhost", 9090, i));
		}
		try {
			List< Future< Boolean > > list = executor.invokeAll(array);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			executor.shutdown();
		   }
		
	}

	public static class MyRunnable implements Callable<Boolean> {

		String ip = null;
		int port = 0;
		int counter = 0;
		public static String[] keyArray = {"A","B","C","D","E"};

		/**
		 * 
		 */
		public MyRunnable(String ip, int port, int counter) {
			// TODO Auto-generated constructor stub
			this.ip = ip;
			this.port = port;
			this.counter = counter;
		}
		
		 MyRunnable(){
			
		}

		static void processResult(Result result, String method,int counter) {
			String errorText = EMPTY_STRING;
			if (null != result) {
				ErrorCode errorCode = result.getError();
				switch (errorCode) {
				case kSuccess:
					String value = result.getValue();
					errorText = result.getErrortext();
					if (method == GET) {
						//System.out.println(value);
						System.out.println("Thread "+counter+" Operation - "+method+" value -"+value+" Sequence number "+getNext());
					}
					if (null != errorText && method == SET) {
						//System.err.println(errorText);
						System.out.println("Thread "+counter+" Operation - "+method+" message "+errorText+" Sequence number "+getNext());
					}
					//System.exit(errorCode.getValue());
					break;

				case kKeyNotFound:
					errorText = result.getErrortext();
					if (null != errorText) {
						//System.err.println(errorText);
						System.out.println("Thread "+counter+" Operation - "+method+" Error"+errorText+" Sequence number "+getNext());
					}
					//System.exit(errorCode.getValue());
					break;

				case kError:
					errorText = result.getErrortext();
					if (null != errorText) {
						//System.err.println(errorText);
						System.out.println("Thread "+counter+" Operation - "+method+" Error"+errorText+" Sequence number "+getNext());
					}
					//System.exit(errorCode.getValue());
					break;
				}
			} else {
				System.out.println("Empty Result");
				//System.exit(2);
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

				if (d < 0.5) {
					operation = GET;
					key = keyArray[ThreadLocalRandom.current().nextInt(0, 5)];
					System.out.println("Thread "+counter+" Operation - "+operation+" key -"+key+" Sequence number "+getNext());
				} else {
					operation = SET;
					key = keyArray[ThreadLocalRandom.current().nextInt(0, 5)];
					value = String.valueOf((int)Math.floor(d*100));
					System.out.println("Thread "+counter+" Operation - "+operation+ " key -"+key+ "   value -"+value+" Sequence number "+getNext());
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
					processResult(res, GET,mr.getCounter());
					break;
				case SET:
					res = client.kvset(key, value);
					processResult(res, SET, mr.getCounter());
					break;
				case DEL:
					res = client.kvdelete(key);
					processResult(res, DEL, mr.getCounter());
					break;

				default:
					break;
				}
				//System.out.println("Fuck!!!");
				transport.close();
			}

			catch (TTransportException e) {
				// e.printStackTrace();
				System.out.println("Server not Running");
				System.exit(2);
			} catch (Exception e) {
				System.out.println("Error!! Thread Number "+counter);
			}
			return true;
		}

		public int getCounter() {
			return counter;
		}

		public void setCounter(int counter) {
			this.counter = counter;
		}

	}
}
