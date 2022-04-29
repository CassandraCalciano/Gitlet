package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;


/** Commit object.
 *  @author Cassandra Calciano
 */
public class Commits implements Serializable {

    /** Hashmap that saves the fileName and blob version. */
    private HashMap _files = new HashMap<String, String>();

    /** Commit message. */
    private String _message = new String();

    /** ID's associated with each Commit. */
    private String _commitID = new String();

    /** Date as a date object. */
    private Date _commitDate = new Date();

    /** Folder to hold all branches in. */
    private String _parentSha = new String();

    /** Constructs a commit object.
     * @param message a message
     * @param toCommit a hashmap to commit
     * @param parentSha a string of sha */
    public Commits(String message, HashMap toCommit, String parentSha) {
        _parentSha = parentSha;
        _files = toCommit;
        _message = message;
        _commitDate = new Date();
        _commitID = Utils.sha1(Utils.serialize(this));
    }

    /** Returns a Hashmap of file names and their sha'd blobs. */
    public HashMap<String, String> files() {
        return _files;
    }

    /** Outputs the message of a commit.
     * @return _message */
    public String getMessage() {
        return _message;
    }

    /** Outputs the commit ID.
     * @return _commitID */
    public String getCommitID() {
        return _commitID;
    }

    /** Outputs the Date of a commit.
     * @return the commit date */
    public Date getCommitDate() {
        return _commitDate;
    }

    /** Outputs the parentSha.
     * @return the parent sha. */
    public String getParentSha() {
        return _parentSha;
    }

    /** Used in init to initialize the Date. */
    public void setCommitFirstDate() {
        _commitDate = new Date(0);
    }

}

