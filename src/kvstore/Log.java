
package kvstore;

public class Log {

	int starttime;
	int endtime;
	String operation;
	String key;
	String value;
	int uniqueIdentifier;
	int dictatingWrite = 0;

	public int getDictatingWrite() {
		return dictatingWrite;
	}

	public void setDictatingWrite(int dictatingWrite) {
		this.dictatingWrite = dictatingWrite;
	}

	public int getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	public void setUniqueIdentifier(int uniqueIdentifier) {
		this.uniqueIdentifier = uniqueIdentifier;
	}

	public int getStarttime() {
		return starttime;
	}

	public void setStarttime(int starttime) {
		this.starttime = starttime;
	}

	public int getEndtime() {
		return endtime;
	}

	public void setEndtime(int endtime) {
		this.endtime = endtime;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "Log [starttime=" + starttime + ", endtime=" + endtime + ", operation=" + operation + ", key=" + key
				+ ", value=" + value + ", UID = " + uniqueIdentifier + "]";
	}
}
