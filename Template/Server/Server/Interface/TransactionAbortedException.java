package Server.Interface;

public class TransactionAbortedException extends Exception {
  public TransactionAbortedException() { super(); }
  public TransactionAbortedException(String message) { super(message); }
  public TransactionAbortedException(String message, Throwable cause) { super(message, cause); }
  public TransactionAbortedException(Throwable cause) { super(cause); }
}