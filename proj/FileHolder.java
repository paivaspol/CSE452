/**
 * A file holder so that we can change the file in memory instead of changing the physical file
 * 
 */
public class FileHolder {

  private String content;
  private boolean isDeleted;
  private final int version;
  private boolean isReadOperation;

  public FileHolder(String content, int version, boolean isReadOperation) {
    isDeleted = false;
    this.content = content;
    this.version = version;
    this.isReadOperation = isReadOperation;
  }

  /**
   * @return the content of the file
   */
  public String getContent() {
    return content;
  }

  /**
   * sets the content of the file
   * 
   * @param content the content of the file
   */
  public void setContent(String content) {
    this.content = content;
  }

  /**
   * @return whether the file is deleted or not
   */
  public boolean isDeleted() {
    return isDeleted;
  }

  /**
   * Sets the deletion status of the file
   * 
   * @param isDeleted
   */
  public void setDeleted(boolean isDeleted) {
    this.isDeleted = isDeleted;
  }

  public int getVersion() {
    return version;
  }

  public void setIsReadOperation(boolean isReadOperation) {
    this.isReadOperation = isReadOperation;
  }

  public boolean isReadOperation() {
    return isReadOperation;
  }
}
