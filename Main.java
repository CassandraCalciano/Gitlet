package gitlet;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import static java.util.Collections.sort;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *
 *  @author Cassandra Calciano
 *
 */
public class Main {

    /** Folder where .gitlet will be held. */
    static final File CURRENTDIRECTORY = new File("");

    /** Folder to hold all files in. */
    static final File GIT = Utils.join(CURRENTDIRECTORY, ".gitlet");

    /** Folder to hold all branches in. */
    static final File BRANCHES = Utils.join(GIT, "branches");

    /** Folder to hold all blobs. */
    static final File BLOBS = Utils.join(GIT, "blobs");

    /** Folder to hold all commit objects. */
    static final File COMMITS = Utils.join(GIT, "commits");

    /** Folder to save the files that we are
     * ready to commit and the files we will delete. */
    static final File STAGINGFOLDER = Utils.join(GIT, "staging");

    /** Folder to save the files we are staging for addition. */
    static final File ADD = Utils.join(STAGINGFOLDER, "add");

    /** Folder to save the files we will delete. */
    static final File DELETE = Utils.join(STAGINGFOLDER, "delete");

    /** File with the name of the branch
     * that HEAD is pointing to written. */
    static final File HEAD = Utils.join(GIT, "HEAD");


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {

        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }

        if (args[0].equals("init")) {
            init();
        } else if (args[0].equals("add")) {
            try {
                add(args[1]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (args[0].equals("commit")) {
            if (args.length == 1) {
                System.out.println("Please enter a commit message.");
            }
            commit(args[1]);
        } else if (args[0].equals("checkout")) {
            checkout(args);
        } else if (args[0].equals("log")) {
            log();
        } else if (args[0].equals("branch")) {
            branch(args[1]);
        } else if (args[0].equals("find")) {
            find(args[1]);
        } else if (args[0].equals("rm")) {
            rm(args[1]);
        } else if (args[0].equals("global-log")) {
            globalLog();
        } else if (args[0].equals("status")) {
            status();
        } else if (args[0].equals("rm-branch")) {
            rmBranch(args[1]);
        } else if (args[0].equals("reset")) {
            reset(args[1]);
        } else {
            System.out.println("No command with that name exists.");
        }
    }

    /** Helper function for init method
     * to initialize instance variables. */
    public static void setupPersistance() {
        GIT.mkdir();
        BRANCHES.mkdir();
        BLOBS.mkdir();
        COMMITS.mkdir();
        STAGINGFOLDER.mkdir();

        try {
            DELETE.createNewFile();
            ADD.createNewFile();
        } catch (IOException e) {
            return;
        }

        Utils.writeObject(ADD, new HashMap<File, String>());
        Utils.writeObject(DELETE, new ArrayList<File>());

    }

    /** Creates a .gitlet folder where all files will be held.
     * Creates one commit that is empty. */
    public static void init() {

        if (GIT.exists()) {
            System.out.println("Gitlet version-control "
                    + "system already exists in the current directory.");
            return;
        }

        setupPersistance();
        Commits initialCommit = new Commits("initial commit",
                new HashMap<String, String>(), null);
        File commit = Utils.join(COMMITS, initialCommit.getCommitID());
        try {
            commit.createNewFile();
        } catch (IOException e) {
            return;
        }

        initialCommit.setCommitFirstDate();

        Utils.writeObject(commit, initialCommit);
        File master = Utils.join(BRANCHES, "master");

        try {
            master.createNewFile();
        } catch (IOException e) {
            return;
        }
        Utils.writeContents(master, initialCommit.getCommitID());
        try {
            HEAD.createNewFile();
        } catch (IOException e) {
            return;
        }
        Utils.writeContents(HEAD, "master");
    }

    /** Commits files, default are the parent files.
     * @param message */
    public static void commit(String message) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
        }

        String headBranch = Utils.readContentsAsString(HEAD);
        String commitID = Utils.readContentsAsString(
                Utils.join(BRANCHES, headBranch));
        Commits parentCommit = Utils.readObject(
                Utils.join(COMMITS, commitID), Commits.class);

        HashMap parentBlob = parentCommit.files();
        HashMap newBlob = Utils.readObject(ADD, HashMap.class);
        if (parentBlob.equals(newBlob)) {
            System.out.println("No changes added to the commit.");
        } else {
            parentBlob.putAll(newBlob);
        }

        ArrayList deletedBlobs = Utils.readObject(DELETE, ArrayList.class);
        for (int i = 0; i < deletedBlobs.size(); i += 1) {
            parentBlob.remove(deletedBlobs.get(i));
        }

        Utils.writeObject(ADD, new HashMap<File, String>());
        Utils.writeObject(DELETE, new ArrayList<File>());

        Commits newCommit = new Commits(message,
                parentBlob, parentCommit.getCommitID());

        File serCommit = Utils.join(COMMITS, newCommit.getCommitID());
        try {
            serCommit.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(serCommit, newCommit);
        Utils.writeContents(Utils.join(BRANCHES, headBranch),
                newCommit.getCommitID());

    }

    /** Adds a copy of the file as it currently exists to
     * the staging area. These added files will be commited.
     * @param fileName */
    public static void add(String fileName) throws IOException {

        File toAdd = new File(fileName);
        if (!(toAdd.exists())) {
            System.out.println("File does not exist.");
            return;
        }

        HashMap staging = Utils.readObject(ADD, HashMap.class);
        Blob toAddBlob = new Blob(fileName);

        String toAddBlobSha = toAddBlob.saveBlob();
        staging.put(fileName, toAddBlobSha);


        String headBranch = Utils.readContentsAsString(HEAD);
        String commitID = Utils.readContentsAsString(
                Utils.join(BRANCHES, headBranch));
        Commits parentCommit = Utils.readObject(
                Utils.join(COMMITS, commitID), Commits.class);

        HashMap parentFiles = parentCommit.files();

        if (parentFiles.containsKey(fileName)) {
            if (toAddBlobSha.equals(parentFiles.get(fileName))) {
                staging.remove(fileName);
            }
        }

        ArrayList toBeRead = Utils.readObject(DELETE, ArrayList.class);
        toBeRead.remove(fileName);
        Utils.writeObject(ADD, staging);
        Utils.writeObject(DELETE, toBeRead);

    }

    /** First Checkout: Takes the version
     * of the file as it exists
     * in the head commit, puts it in the working directory,
     * overwriting the version of the file that's
     * already there if there is one. The new version
     * of the file is not staged.
     * @param args */
    public static void checkout(String[] args) {
        if (args.length == 3) {
            String fileName = args[2];
            File fileToUpdate = new File(fileName);
            String headBranch = Utils.readContentsAsString(HEAD);
            String commitID = Utils.readContentsAsString(
                    Utils.join(BRANCHES, headBranch));
            Commits parentCommit = Utils.readObject(
                    Utils.join(COMMITS, commitID), Commits.class);
            HashMap parentFiles = parentCommit.files();
            if (!parentFiles.containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                return;
            }
            String blobShaName = (String) parentFiles.get(fileName);
            Blob contentsOfParent = Utils.readObject(
                    Utils.join(BLOBS, blobShaName), Blob.class);
            Utils.writeContents(fileToUpdate, contentsOfParent.getContents());
        }
        if (args.length == 4) {
            String commitID = args[1];
            if (commitID.equals("")) {
                System.out.println("No commit with that id exists.");
                return;
            }
            String fileName = args[3];
            File fileToUpdate = new File(fileName);
            Commits parentCommit = Utils.readObject(
                    Utils.join(COMMITS, commitID), Commits.class);
            HashMap parentFiles = parentCommit.files();
            if (!parentFiles.containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                return;
            }
            String blobShaName = (String) parentFiles.get(fileName);
            Blob contentsOfParent = Utils.readObject(
                    Utils.join(BLOBS, blobShaName), Blob.class);
            Utils.writeContents(fileToUpdate, contentsOfParent.getContents());
        }
        if (args.length == 2) {
            checkout3(args);
        }
    }

    /** Third if statement from checkout.
     * @param args an arguement from tne original checkout*/
    public static void checkout3(String[] args) {
        String branchName = args[1];
        String headBranch = Utils.readContentsAsString(HEAD);
        String commitID = Utils.readContentsAsString(
                Utils.join(BRANCHES, headBranch));
        Commits parentCommit = Utils.readObject(
                Utils.join(COMMITS, commitID), Commits.class);
        if (branchName.equals(headBranch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        File check = Utils.join(BRANCHES, branchName);
        if (!check.exists()) {
            System.out.println("No such branch exists.");
            return;
        }
        String commitChBranch = Utils.readContentsAsString(
                Utils.join(BRANCHES, branchName));
        Commits commitCh = Utils.readObject(
                Utils.join(COMMITS, commitChBranch), Commits.class);
        for (String file : commitCh.files().keySet()) {
            File fileA = Utils.join(CURRENTDIRECTORY, file);
            if (fileA.exists() && !parentCommit.files().containsKey(file)) {
                System.out.println("There is an untracked "
                        + "file in the way; delete it, "
                        + "or add and commit it first.");
                return;
            }
        }
        Utils.writeContents(HEAD, branchName);
        for (String fileName : commitCh.files().keySet()) {
            File newFile = Utils.join(CURRENTDIRECTORY, fileName);
            String blobName = commitCh.files().get(fileName);
            String blob = Utils.readContentsAsString(
                    Utils.join(BLOBS, blobName));
            Utils.writeContents(newFile, blob);
        }
        for (String fileName : parentCommit.files().keySet()) {
            if (!commitCh.files().containsKey(fileName)) {
                File newFile = Utils.join(CURRENTDIRECTORY, fileName);
                newFile.delete();
            }
        }
    }
    /** Prints out information. */
    public static void log() {

        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE MMM d HH:mm:ss yyyy Z");

        String headBranch = Utils.readContentsAsString(HEAD);
        String commitID = Utils.readContentsAsString(
                Utils.join(BRANCHES, headBranch));
        Commits tempCommit = Utils.readObject(
                Utils.join(COMMITS, commitID), Commits.class);

        while (tempCommit != null) {

            String tempID = tempCommit.getCommitID();
            String tempDate = sdf.format(tempCommit.getCommitDate());
            String tempMessage = tempCommit.getMessage();

            System.out.println("===");
            System.out.println("commit " + tempID);
            System.out.println("Date: " + tempDate);
            System.out.println(tempMessage);
            System.out.println();

            if (tempCommit.getParentSha() == null) {
                return;
            }

            tempCommit = Utils.readObject(
                    Utils.join(COMMITS, tempCommit.
                            getParentSha()), Commits.class);
        }
    }

    /** Creates a new branch with name BRANCHNAME.
     * @param branchName */
    public static void branch(String branchName) {

        List<String> existingBranches =
                Utils.plainFilenamesIn(BRANCHES);

        if (existingBranches.contains(branchName)) {
            System.out.println(
                    "A branch with that name already exists.");
            return;
        }

        String headBranch = Utils.readContentsAsString(HEAD);
        String commitID = Utils.readContentsAsString(
                Utils.join(BRANCHES, headBranch));

        File newBranch = Utils.join(BRANCHES, branchName);
        try {
            newBranch.createNewFile();
        } catch (IOException e) {
            return;
        }

        Utils.writeContents(newBranch, commitID);
    }

    /** Usage: java gitlet.Main rm [file name].
     * @param fileName */
    public static void rm(String fileName) {
        String headBranch = Utils.readContentsAsString(HEAD);
        String commitID = Utils.readContentsAsString(
                Utils.join(BRANCHES, headBranch));
        Commits currCommit = Utils.readObject(
                Utils.join(COMMITS, commitID), Commits.class);

        HashMap contentsToCheck = Utils.readObject(ADD, HashMap.class);
        boolean inStaging = false;
        if (contentsToCheck.containsKey(fileName)) {
            contentsToCheck.remove(fileName);
            Utils.writeObject(ADD, contentsToCheck);
            inStaging = true;
        }
        if (currCommit.files().get(fileName) != null) {
            ArrayList toDelete = Utils.readObject(DELETE, ArrayList.class);
            toDelete.add(fileName);
            Utils.writeObject(DELETE, toDelete);
            Utils.restrictedDelete(fileName);
            inStaging = true;
        }
        if (!inStaging) {
            System.out.println("No reason to remove the file.");
        }
    }

    /** Usage: java gitlet.Main rm-branch [branch name].
     * @param branchName */
    public static void rmBranch(String branchName) {

        File branch = Utils.join(BRANCHES, branchName);
        if (!branch.exists()) {
            System.out.println(
                    "A branch with that name does not exist.");
            return;
        }

        String headBranch = Utils.readContentsAsString(HEAD);
        if (headBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }

        branch.delete();

    }

    /** Usage: java gitlet.Main find [commit message].
     * @param givenMessage */
    public static void find(String givenMessage) {

        int num = 0;

        List<String> allCommits = Utils.plainFilenamesIn(COMMITS);
        for (int i = 0; i < allCommits.size(); i += 1) {
            String commitName = allCommits.get(i);
            Commits commit = Utils.readObject(
                    Utils.join(COMMITS, commitName), Commits.class);

            if (commit.getMessage().equals(givenMessage)) {
                num += 1;
                System.out.println(commit.getCommitID());
            }
        }

        if (num == 0) {
            System.out.println(
                    "Found no commit with that message.");
            return;
        }

    }

    /** Usage: java gitlet.Main global-log. */
    public static void globalLog() {

        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE MMM d HH:mm:ss yyyy Z");

        List<String> allCommits = Utils.plainFilenamesIn(COMMITS);
        for (int i = 0; i < allCommits.size(); i += 1) {
            String commitName = allCommits.get(i);
            Commits tempCommit = Utils.readObject(
                    Utils.join(COMMITS, commitName), Commits.class);

            String tempID = tempCommit.getCommitID();
            String tempDate = sdf.format(tempCommit.getCommitDate());
            String tempMessage = tempCommit.getMessage();

            System.out.println("===");
            System.out.println("commit " + tempID);
            System.out.println("Date: " + tempDate);
            System.out.println(tempMessage);
            System.out.println();

        }
    }

    /** Usage: java gitlet.Main status. */
    public static void status() {

        if (!GIT.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        String headBranch = Utils.readContentsAsString(HEAD);
        List<String> allBranches =
                Utils.plainFilenamesIn(BRANCHES);
        ArrayList branches = new ArrayList();
        branches.addAll(allBranches);

        branches.remove(headBranch);
        branches.add("*" + headBranch);

        sort(branches);

        System.out.println("=== Branches ===");
        for (int i = 0; i < branches.size(); i += 1) {
            System.out.println(branches.get(i));
        }
        System.out.println();

        HashMap stagedHash = Utils.readObject(ADD, HashMap.class);
        Set stagedSet = stagedHash.keySet();
        List<String> stagedFiles = new ArrayList<String>();

        for (Object file : stagedSet) {
            stagedFiles.add((String) file);
        }
        sort(stagedFiles);

        System.out.println("=== Staged Files ===");
        for (int i = 0; i < stagedFiles.size(); i += 1) {
            System.out.println(stagedFiles.get(i));
        }
        System.out.println();

        ArrayList<String> deletedFiles =
                Utils.readObject(DELETE, ArrayList.class);
        sort(deletedFiles);

        System.out.println("=== Removed Files ===");
        for (int i = 0; i < deletedFiles.size(); i += 1) {
            System.out.println(deletedFiles.get(i));
        }
        System.out.println();

        System.out.println("=== Modifications Not "
                + "Staged For Commit ===");
        System.out.println();

        System.out.println("=== Untracked Files ===");

    }

    /** Usage: java gitlet.Main reset [commit id].
     * @param givenID */
    public static void reset(String givenID) {

        Commits commitCh;

        List<String> allCommits = Utils.
                plainFilenamesIn(COMMITS);
        if (!allCommits.contains(givenID)) {
            System.out.println(
                    "No commit with that id exists.");
            return;
        }

        String headBranch = Utils.readContentsAsString(HEAD);
        String commitID = Utils.readContentsAsString(
                Utils.join(BRANCHES, headBranch));
        Commits parentCommit = Utils.readObject(
                Utils.join(COMMITS, commitID), Commits.class);

        commitCh = Utils.readObject(Utils.join(
                COMMITS, givenID), Commits.class);

        for (String file : commitCh.files().keySet()) {
            File fileA = Utils.join(CURRENTDIRECTORY, file);
            if (fileA.exists() && !parentCommit.files().containsKey(file)) {
                System.out.println("There is an untracked file in "
                        + "the way; delete it, or add and commit it first.");
                return;
            }
        }

        for (String fileName : commitCh.files().keySet()) {
            File newFile = Utils.join(CURRENTDIRECTORY, fileName);
            String blobName = commitCh.files().get(fileName);
            String blob = Utils.readContentsAsString(
                    Utils.join(BLOBS, blobName));
            Utils.writeContents(newFile, blob);
        }
        for (String fileName : parentCommit.files().keySet()) {
            if (!commitCh.files().containsKey(fileName)) {
                File newFile = Utils.join(CURRENTDIRECTORY, fileName);
                newFile.delete();
            }
        }
        File file = Utils.join(BRANCHES, headBranch);
        Utils.writeContents(file, givenID);

        Utils.writeObject(ADD, new HashMap<File, String>());
        Utils.writeObject(DELETE, new ArrayList<File>());

    }


}



