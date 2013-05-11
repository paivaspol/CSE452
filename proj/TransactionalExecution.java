import java.util.HashMap;
import java.util.Map;

public class TransactionalExecution {

	private final StringBuilder logBuffer;
	private final Map<String, FileHolder> modifiedFiles; // a mapping between the filename and the content of the new file

	public TransactionalExecution() {
		logBuffer = new StringBuilder();
		modifiedFiles = new HashMap<String, FileHolder>();
	}

	/**
	 * adds a log entry to the buffer
	 * 
	 * @param tid the transaction id
	 * @param filename the filename
	 * @param method the method
	 * @param value the value. This is optional. Can be null if not wanted.
	 */
	public void addLogEntry(long tid, String filename, String method, String value) {
		String entry = tid + '\t' + filename + '\t' + method;
		if (value != null) {
			entry += '\t' + value;
		}
		entry += '\n';
		logBuffer.append(entry);
	}

	/**
	 * replaces the file's content with the new content
	 * 
	 * @param filename
	 * @param newContent
	 */
	public void modifyFile(String filename, String newContent) {
		FileHolder fileHolder = modifiedFiles.get(filename);
		if (fileHolder != null) {
			modifiedFiles.get(filename).setContent(newContent);	
		} else {
			modifiedFiles.put(filename, new FileHolder(newContent));
		}
	}

	/**
	 * Deletes the file by triggering the deleted flag to true
	 * 
	 * @param filename of the file to be deleted
	 */
	public void deleteFile(String filename) {
		FileHolder fileHolder = modifiedFiles.get(filename);
		if (fileHolder == null) {
			modifiedFiles.put(filename, new FileHolder(""));
		}
		modifiedFiles.get(filename).setDeleted(true);

	}

	public boolean isFileDeleted(String filename) {
		return modifiedFiles.get(filename).isDeleted();
	}

	/**
	 * @param filename
	 * @return the content of the file, null if this transaction hasn't deal with it yet
	 */
	public String readFile(String filename) {
		FileHolder fileHolder =   modifiedFiles.get(filename);
		if (fileHolder != null) {
			return fileHolder.getContent();
		} else {
			return null;
		}
	}

	/**
	 * @return the whole log file
	 */
	public String getLogContent() {
		return logBuffer.toString();
	}

	/**
	 * @return all the files that were modified
	 */
	public Map<String, FileHolder> getAllModfiedFiles() {
		return new HashMap<String, FileHolder>(modifiedFiles);
	}
}
