package gitlet;

import java.io.File;
import java.io.Serializable;

/** Blob Obeject.
 *  @author Cassandra Calciano
 */
public class Blob implements Serializable {

    /** Name of File with blob as contents. */
    private String _fileName;

    /** String with the contents of the Blob. */
    private String _contents;

    /** File to hold .gitlet. */
    static final File GIT = new File(".gitlet");

    /** Folder to hold all blobs. */
    static final File BLOBS = Utils.join(GIT, "blobs");


    /** Blob Constructor.
     * @param name */
    public Blob(String name) {
        _fileName = name;
        File fileToBeRead = new File(name);
        _contents = Utils.readContentsAsString(fileToBeRead);
    }

    /** String with the contents of the Blob.
     * @return */
    public String saveBlob() {
        String sha1 = Utils.sha1(_contents);
        File sha1File = Utils.join(BLOBS, sha1);
        Utils.writeObject(sha1File, this);
        return sha1;
    }

    /** String with the contents of the Blob.
     * @return */
    public String getContents() {
        return _contents;
    }
}
