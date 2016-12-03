package kvstore;

import java.util.HashMap;

import org.apache.thrift.TException;

public class KVStoreHandler implements KVStore.Iface {

 static final HashMap <String, String> kvStoreMap = new HashMap <String, String>();
	
 @Override
 public Result kvset(String key, String value) throws TException {

  	kvStoreMap.put(key, value);
  	return new Result("", ErrorCode.kSuccess, "Set success"); 
 }

 @Override 
 public Result kvget(String key) throws org.apache.thrift.TException {
	 if(kvStoreMap.containsKey(key)) {
		 return new Result(kvStoreMap.get(key), ErrorCode.kSuccess, "Get success");
	 }
	 else {
		 String errorText = key + " not found on Server";
		 return new Result(null, ErrorCode.kKeyNotFound, errorText);
	 }
 }

 @Override
 public Result kvdelete(String key) throws org.apache.thrift.TException {
 	if(kvStoreMap.containsKey(key)) {
 		String value = kvStoreMap.remove(key);
 		return new Result(value, ErrorCode.kSuccess, "Delete success");
 	}
 	else {
 		String errorText = key + " not found on Server";
 		return new Result(null, ErrorCode.kKeyNotFound, errorText);
 	}
 }
}